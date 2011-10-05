// docbook-editor.js

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
	$('#wym-iframe').one('load', function() {
		WYMeditor.INSTANCES[0].initIframe(this);
	});
	$('.wym_box_0').wymeditor({
		html: '',
		lang: "en",
		initSkin: false,
		loadSkin: false,
		basePath: '/callimachus/editor/wymeditor/',
		skinPath: '/callimachus/editor/wymeditor/',
		jQueryPath: '/callimachus/scripts/jquery.js',
		wymPath: '/callimachus/editor/wymeditor/jquery.wymeditor.js',
		dialogFeatures: 'jQuery.dialog',
		dialogFeaturesPreview: 'jQuery.dialog',
	});
	$('#wym-iframe').one('load', function() {
		WYMeditor.INSTANCES[0].initIframe(this);

		function getPageLocationURL() {
			// window.location.href needlessly decodes URI-encoded characters in the URI path
			// https://bugs.webkit.org/show_bug.cgi?id=30225
			var path = location.pathname;
			if (path.match(/#/))
				return location.href.replace(path, path.replace('#', '%23'));
			return location.href;
		}

		var editor = jQuery.wymeditors(0);
		var path = null;
		var contentType = null;
		var etag = null;

		$(window).bind('message', function(event) {
			var data = event.originalEvent.data;
			if ($('#image-iframe').length && event.originalEvent.source == $('#image-iframe')[0].contentWindow && data.indexOf('PUT src\n') == 0) {
				var src = data.substring(data.indexOf('\n\n') + 2);
				var uri = calli.listResourceIRIs(src)[0];
				if (uri.search(/\.[a-zA-Z]+$/) > 0) {
					editor.insert("<img src='" + uri + "' />");
					if (window.dialog) {
						window.dialog.dialog('close');
					}
				}
			}
		});

		// loading
		function onhashchange() {
			if (location.hash && location.hash.length > 1) {
				if (location.hash.indexOf('!') > 0) {
					path = location.hash.substring(location.hash.indexOf('!') + 1);
				}
				if (path && !editor.html()) {
					jQuery.ajax({type: 'GET', url: path, complete: function(xhr) {
						if (xhr.status == 200 || xhr.status == 304) {
							contentType = xhr.getResponseHeader('Content-Type');
							etag = xhr.getResponseHeader('ETag');
							var body = xhr.responseText;
							if (body != editor.docbook()) {
								editor.docbook(body);
							}
						}
					}});
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
			var text = editor.docbook();
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
			var text = editor.docbook();
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
				if (!editor.html()) {
					editor.docbook(body);
				}
				return true;
			} else if (header == 'GET line.column') {
				return '';
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
		parent.postMessage('CONNECT calliEditorLoaded', '*');
	});
});
