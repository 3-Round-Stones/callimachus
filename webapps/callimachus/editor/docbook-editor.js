// docbook-editor.js

jQuery(function($) {
	var jQuery = $;
	$('.wym_box_0').wymeditor({
		html: '',
		lang: "en",
		initSkin: false,
		loadSkin: false,
		basePath: calli.getCallimachusUrl('editor/wymeditor/'),
		skinPath: calli.getCallimachusUrl('editor/wymeditor/'),
		jQueryPath: calli.getCallimachusUrl('scripts/jquery.js'),
		wymPath: calli.getCallimachusUrl('editor/wymeditor/jquery.wymeditor.js'),
		dialogFeatures: navigator.appName == 'Microsoft Internet Explorer' ? undefined : 'jQuery.dialog',
		dialogFeaturesPreview: navigator.appName == 'Microsoft Internet Explorer' ? undefined : 'jQuery.dialog'
	});
	$('#wym-iframe').one('load', function() {
		WYMeditor.INSTANCES[0].initIframe(this);

		var editor = jQuery.wymeditors(0);
		var path = null;
		var contentType = null;
		var etag = null;

		if ($('#wym-iframe')[0].contentWindow.document.body) {
			onhashchange();
		} else {
			setTimeout(function(){
				if (!$('body', $('#wym-iframe')[0].contentWindow.document).children().length) {
					// IE need this called twice on first load
					WYMeditor.INSTANCES[0].initIframe($('#wym-iframe')[0]);
					// IE8 hides the iframe body on input, resizing it causes it to show again
					editor = jQuery.wymeditors(0);
					var iframe = $('#wym-iframe');
					var resetHeight = function() {
						iframe.css('height', iframe.height() + 'px').css('box-sizing', 'content-box');
						setTimeout(setFullHeight, 300);
					};
					var setFullHeight = function() {
						iframe.css('height', '100%').css('box-sizing', 'border-box');
						setTimeout(resetHeight, 300);
					};
					setFullHeight();
				}
				onhashchange();
			}, 1000);
		}

		// loading
		$(window).bind('hashchange', onhashchange);
		function onhashchange() {
			if (location.hash && location.hash.length > 1) {
				if (location.hash.indexOf('!') > 0) {
					path = location.hash.substring(location.hash.indexOf('!') + 1);
				}
				var html = editor.html();
				if (path && !(html && html.replace(/<[^>]*>/,'').replace(/\s+/,''))) {
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
					if (xhr.status == 204 || xhr.status == 1223) {
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
					if (xhr.status == 204 || xhr.status == 1223) {
						etag = xhr.getResponseHeader('ETag');
					}
					if (typeof callback == 'function') {
						callback(xhr);
					}
				}
			});
			return true;
		}

		// messaging
		function handleMessage(header, body) {
			if (header.indexOf('POST create\n') == 0) {
				var m = header.match(/^POST create\s+Action:\s*(\S*)(\s+Content-Type:\s*(\S*))?\b/i);
				var action = m[1];
				if (m[3]) {
					contentType = m[3];
				}
				postFile(action, function(xhr) {
					if (xhr.status < 300 || xhr.status == 1223) {
						parent.postMessage('OK\n\n' + header + '\n\n' + xhr.getResponseHeader('Location'), '*');
					} else {
						parent.postMessage('ERROR ' + xhr.statusText + '\n\n' + header + '\n\n' + xhr.responseText, '*');
					}
				});
				return false; // don't respond yet
			} else if (header == 'POST save') {
				putFile(function(xhr) {
					if (xhr.status < 300 || xhr.status == 1223) {
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
		if (window.parent != window) {
			parent.postMessage('CONNECT calliEditorLoaded', '*');
		}
	});
	$('#wym-iframe')[0].src = calli.getCallimachusUrl("editor/wymeditor/wymiframe.html");
});
