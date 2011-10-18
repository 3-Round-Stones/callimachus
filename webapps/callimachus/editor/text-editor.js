// text-editor.js

(function($) {
	$(window).ajaxSend(function(event, XMLHttpRequest, ajaxOptions) {
		$('#editor').css('background-color', 'lightyellow');
	});
	$(window).ajaxSuccess(function(event, XMLHttpRequest, ajaxOptions) {
		$('#editor').css('background-color', 'inherit');
	});
	$(window).ajaxError(function(event, XMLHttpRequest, ajaxOptions) {
		$('#editor').css('background-color', '#FF9999');
		setTimeout(function() {
			$('#editor').css('background-color', 'inherit');
		}, 1000);
	});
})(jQuery);

jQuery(function($) {
	var editor = ace.edit("editor");
	var path = null;
	var contentType = null;
	var etag = null;

	var onresize = function() {
		setTimeout(function() {
			var pane = $('.ace_scroller')[0];
			if (pane.scrollWidth > pane.clientWidth && window.parent != window) {
				var width = pane.scrollWidth - pane.clientWidth + $(pane).outerWidth(true);
				width += $('.ace_gutter').outerWidth(true);
				width += 32; // scrollbar width
				$(pane).parents().each(function() {
					width += $(this).outerWidth(true) - $(this).width();
				});
				parent.postMessage('PUT width\n\n' + width, '*');
			}
		}, 100);
	};
	$(window).bind('resize', onresize);

	// loading
	function onhashchange() {
		if (location.hash && location.hash.length > 1) {
			var mode, line = null, column = null;
			if (location.hash.indexOf('!') > 0 && location.hash.indexOf('#', 2) > 0) {
				mode = location.hash.substring(1, location.hash.indexOf('!'));
				path = location.hash.substring(location.hash.indexOf('!') + 1, location.hash.indexOf('#', 2));
				line = location.hash.substring(location.hash.indexOf('#', 2) + 1);
				if (line.indexOf('.')) {
					column = line.substring(line.indexOf('.') + 1);
					line = line.substring(0, line.indexOf('.'));
				}
			} else if (location.hash.indexOf('!') > 0) {
				mode = location.hash.substring(1, location.hash.indexOf('!'));
				path = location.hash.substring(location.hash.indexOf('!') + 1);
			} else {
				mode = location.hash.substring(1);
				path = null;
			}
			if (mode) {
				var Mode = require("ace/mode/" + mode).Mode;
				if (Mode) {
					editor.getSession().setMode(new Mode());
				}
			}
			if (path && !editor.getSession().getValue()) {
				jQuery.ajax({type: 'GET', url: path, complete: function(xhr) {
					if (xhr.status == 200 || xhr.status == 304) {
						contentType = xhr.getResponseHeader('Content-Type');
						etag = xhr.getResponseHeader('ETag');
						var body = xhr.responseText;
						var row = editor.getSelectionRange().start.row;
						var col = editor.getSelectionRange().start.column;
						if (body != editor.getSession().getValue()) {
							editor.getSession().setValue(body);
							onresize();
						}
						if (line && line != editor.getSelectionRange().start.row + 1) {
							editor.gotoLine(line, column);
						} else if (line && column && column != editor.getSelectionRange().start.column) {
							editor.gotoLine(line, column);
						} else if (row != editor.getSelectionRange().start.row) {
							editor.gotoLine(row + 1, col);
						}
					}
				}});
			} else if (line && line != editor.getSelectionRange().start.row + 1) {
				editor.gotoLine(line, column);
			} else if (line && column && column != editor.getSelectionRange().start.column) {
				editor.gotoLine(line, column);
			}
		}
	}
	$(window).bind('hashchange', onhashchange);
	onhashchange();

	// saving
	var saving = false;
	function postFile(action, callback) {
		if (saving) return false;
		saving = true;
		var text = editor.getSession().getValue();
		jQuery.ajax({
			type: 'POST',
			url: action,
			contentType: contentType,
			data: text,
			complete: function(xhr) {
				saving = false;
				if (xhr.status == 204) {
					etag = xhr.getResponseHeader('ETag');
				}
				if (typeof callback == 'function') {
					callback(xhr);
				}
			}
		});
		return true;
	}
	function putFile(callback) {
		var text = editor.getSession().getValue();
		if (saving) return false;
		saving = true;
		jQuery.ajax({
			type: 'PUT',
			url: path,
			contentType: contentType,
			beforeSend: function(xhr) {
				if (etag) {
					xhr.setRequestHeader('If-Match', etag);
				}
			},
			data: text,
			complete: function(xhr) {
				saving = false;
				if (xhr.status == 204) {
					etag = xhr.getResponseHeader('ETag');
				}
				if (typeof callback == 'function') {
					callback(xhr);
				}
			}
		});
		return true;
	}
	require('pilot/canon').addCommand({
		name: 'save',
		bindKey: {
		    win: 'Ctrl-S',
		    mac: 'Command-S',
		    sender: 'editor'
		},
		exec: function(env, args, request) {
		    putFile();
		}
	});

	// messaging
	function handleMessage(header, body) {
		if (header.indexOf('POST create\n') == 0) {
			var m = header.match(/^POST create\s+Action:\s*(\S*)(\s+Content-Type:\s*(\S*))?\b/i);
			var action = m[1];
			if (m[3]) {
				contentType = m[3];
			}
			postFile(action, function(xhr) {
				if (xhr.status < 300) {
					parent.postMessage('OK\n\n' + header + '\n\n' + xhr.getResponseHeader('Location'), '*');
				} else {
					parent.postMessage('ERROR ' + xhr.statusText + '\n\n' + header + '\n\n' + xhr.responseText, '*');
				}
			});
			return false; // don't respond yet
		} else if (header == 'POST save') {
			putFile(function(xhr) {
				if (xhr.status < 300) {
					parent.postMessage('OK\n\n' + header, '*');
				} else {
					parent.postMessage('ERROR ' + xhr.statusText + '\n\n' + header + '\n\n' + xhr.responseText, '*');
				}
			});
			return false; // don't respond yet
		} else if (header == 'POST template' && body) {
			if (!editor.getSession().getValue()) {
				editor.insert(body);
				onresize();
			}
			return true;
		} else if (header == 'GET line.column') {
			var start = editor.getSelectionRange().start;
			return '' + (1 + start.row) + '.' + start.column;
		}
		return true; // empty response
	};

	$(window).bind('message', function(event) {
		if (event.originalEvent.source == parent) {
			var msg = event.originalEvent.data;
			var header = msg;
			var body = null;
			if (msg.indexOf('\n\n') > 0) {
				header = msg.substring(0, msg.indexOf('\n\n'));
				body = msg.substring(msg.indexOf('\n\n') + 2);
			}
			try {
				var response = handleMessage(header, body);
				if (!response && typeof response == 'boolean') {
					// respond later
				} else if (response && typeof response != 'boolean') {
					parent.postMessage('OK\n\n' + header + '\n\n' + response, '*');
				} else {
					parent.postMessage('OK\n\n' + header, '*');
				}
			} catch (e) {
				parent.postMessage('ERROR\n\n' + header + '\n\n' + e, '*');
			}
		}
	});
	if (window.parent != window) {
		parent.postMessage('CONNECT calliEditorLoaded', '*');
	}
});
