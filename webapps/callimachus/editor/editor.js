// editor.js

jQuery(function($) {
	$('#editor').ajaxSend(function(event, XMLHttpRequest, ajaxOptions) {
		$('#editor').css('background-color', 'lightyellow');
	});
	$('#editor').ajaxSuccess(function(event, XMLHttpRequest, ajaxOptions) {
		$('#editor').css('background-color', 'inherit');
	});
	$('#editor').ajaxError(function(event, XMLHttpRequest, ajaxOptions) {
		$('#editor').css('background-color', '#FF9999');
		setTimeout(function() {
			$('#editor').css('background-color', 'inherit');
		}, 1000);
	});
});

jQuery(function($) {
	var editor = ace.edit("editor");
	var path = null;
	var contentType = null;
	var etag = null;

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
					if (xhr.status == 200) {
						contentType = xhr.getResponseHeader('Content-Type');
						etag = xhr.getResponseHeader('ETag');
						var body = xhr.responseText;
						var row = editor.getSelectionRange().start.row;
						var col = editor.getSelectionRange().start.column;
						if (body != editor.getSession().getValue()) {
							editor.getSession().setValue(body);
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
			beforeSend: function(xhr) {
				xhr.setRequestHeader('Location', path);
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
	function putFile(callback) {
		var text = editor.getSession().getValue();
		if (!etag)
			throw 'No file loaded';
		if (saving) return false;
		saving = true;
		jQuery.ajax({
			type: 'PUT',
			url: path,
			contentType: contentType,
			beforeSend: function(xhr) {
				xhr.setRequestHeader('If-Match', etag);
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
	$(window).keypress(function(event) {
		if (event.which == 115 && event.ctrlKey || event.which == 19) {
			event.preventDefault();
			putFile();
			return false;
		}
		return true;
	});
	(function() {
		var isCtrl = false;
		$(document).keyup(function (e) {
			if(e.which == 17) isCtrl=false;
			return true;
		});
		$(document).keydown(function (e) {
			if(e.which == 17) isCtrl=true;
			if(e.which == 83 && isCtrl == true) {
				e.preventDefault();
				putFile();
				return false;
			}
			return true;
		});
	})();

	// messaging
	function handleMessage(header, body) {
		if (header.indexOf('POST create\n') == 0) {
			var m = header.match(/^POST create\s+Action:\s*(\S*)\s+Location:\s*(\S*)\s+Content-Type:\s*(\S*)\b/i);
			var action = m[1];
			path = m[2];
			contentType = m[3];
			postFile(action, function(xhr) {
				if (xhr.status < 300) {
					parent.postMessage('OK\n\n' + header + '\n\nLocation: ' + xhr.getResponseHeader('Location'), '*');
				} else {
					parent.postMessage(xhr.statusText + '\n\n' + header + '\n\n' + xhr.responseText, '*');
				}
			});
			return false; // don't respond yet
		} else if (header == 'POST save') {
			putFile(function(xhr) {
				if (xhr.status < 300) {
					parent.postMessage('OK\n\n' + header, '*');
				} else {
					parent.postMessage(xhr.statusText + '\n\n' + header + '\n\n' + xhr.responseText, '*');
				}
			});
			return false; // don't respond yet
		} else if (header == 'GET text') {
			return editor.getSession().getValue();
		} else if (header == 'POST text') {
			if (body != editor.getSession().getValue()) {
				var row = editor.getSelectionRange().start.row;
				var column = editor.getSelectionRange().start.column;
				editor.getSession().setValue(body);
				editor.gotoLine(row + 1, column);
			}
			return true;
		} else if (header == 'POST insert' && body) {
			editor.insert(body);
			return true;
		} else if (header == 'GET line.column') {
			var start = editor.getSelectionRange().start;
			return '' + (1 + start.row) + '.' + start.column;
		} else if (header == 'POST line.column' && body) {
			var line = parseInt(body.split('.', 2)[0]);
			var column = parseInt(body.split('.', 2)[1]);
			var start = editor.getSelectionRange().start;
			if (start.row + 1 != line || start.column != column) {
				editor.gotoLine(line, column);
			}
			return true;
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
	parent.postMessage('CONNECT calliTextEditorLoaded', '*');
});
