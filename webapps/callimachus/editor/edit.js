// edit.js

(function($, jQuery) {
    $(window).ajaxSend(function(event, XMLHttpRequest, ajaxOptions) {
		$('form[enctype]').css('background-color', 'lightyellow');
	});
	$(window).ajaxSuccess(function(event, XMLHttpRequest, ajaxOptions) {
		$('form[enctype]').css('background-color', 'inherit');
	});
	$(window).ajaxError(function(event, XMLHttpRequest, ajaxOptions) {
		$('form[enctype]').css('background-color', '#FF9999');
		setTimeout(function() {
			$('form[enctype]').css('background-color', 'inherit');
		}, 1000);
	});
})(jQuery, jQuery);

(function($){
    var editor = null;
    var etag;
    var original;

	$.ajax({type: 'GET', url: location.pathname, beforeSend: withCredentials, complete: function(xhr) {
		if (xhr.status == 200 || xhr.status == 304) {
			etag = xhr.getResponseHeader('ETag');
			original = xhr.responseText;
            if (editor) {
                editor.postMessage('PUT src\n\n' + original, '*');
            }
		}
	}});
    $(window).bind('message', function(event) {
    	if (event.originalEvent.source == $('#iframe')[0].contentWindow && event.originalEvent.data == 'CONNECT calliEditorLoaded') {
            editor = $('#iframe')[0].contentWindow;
			if (original) {
				editor.postMessage('PUT src\n\n' + original, '*');
			}
			$(window).bind('hashchange', onhashchange);
			onhashchange();
		}
	});
	function onhashchange() {
		if (location.hash && location.hash.length > 1) {
			editor.postMessage('PUT line.column\n\n' + location.hash.substring(1), '*');
		}
	}
    $(document).bind('calliOpenDialog', function(event) {
		if (editor && !event.isDefaultPrevented()) {
			editor.postMessage('PUT disabled\n\ntrue', '*');
		}
	});
	$(document).bind('calliCloseDialog', function(event) {
		if (!event.isDefaultPrevented()) {
			editor.postMessage('PUT disabled\n\nfalse', '*');
		}
	});
    $(window).bind('message', function(event) {
    	if (event.originalEvent.source == editor && event.originalEvent.data.indexOf('PUT src\n\n') == 0) {
            var text = event.originalEvent.data.substring('PUT src\n\n'.length);
			$('form[enctype][method="PUT"]').each(function() {
				var form = this;
				saveFile(form, text);
			});
		}
	});
	var sourceCallbacks = [];
	function getSource(callback) {
		sourceCallbacks.push(callback);
		if (sourceCallbacks.length == 1) {
			editor.postMessage('GET src', '*');
		}
	}
	$(window).bind('message', function(event) {
		if (event.originalEvent.source == editor) {
			var msg = event.originalEvent.data;
			if (msg.indexOf('OK\n\nGET src\n\n') == 0) {
				var text = msg.substring('OK\n\nGET src\n\n'.length);
				var callbacks = sourceCallbacks;
				sourceCallbacks = [];
				for (var i=0; i<callbacks.length; i++) {
					callbacks[i](text);
				}
			}
		}
	});

	// saving
	var saving = false;
	function saveFile(form, text, callback) {
		if (saving) return false;
		saving = true;
		$.ajax({
			type: form.getAttribute('method'),
			url: calli.getFormAction(form),
			contentType: form.getAttribute("enctype"),
			data: text,
			beforeSend: function(xhr) {
				if (etag && form.getAttribute('method') == 'PUT') {
					xhr.setRequestHeader('If-Match', etag);
				}
				withCredentials(xhr);
			},
			complete: function(xhr) {
				saving = false;
				if (xhr.status < 300 || xhr.status == 1223) {
					if (xhr.status == 204 || xhr.status == 1223) {
						etag = xhr.getResponseHeader('ETag');
					}
					if (typeof callback == 'function') {
						callback(xhr);
					}
				}
			}
		});
		return true;
	}
	function withCredentials(req) {
		try {
			req.withCredentials = true;
		} catch (e) {}
	}

jQuery(function($){
	$('form[enctype]').submit(function(event) {
		var form = this;
		var about = $(form).attr('about');
		event.preventDefault();
		getSource(function(text) {
			saveFile(form, text, function(xhr) {
				var url = xhr.getResponseHeader('Location');
				if (url) {
					loadEditor(url + '?edit');
				} else if (about) {
					loadEditor(about + '?edit');
				} else {
					location.replace('?view');
				}
			});
		});
		return false;
	});
	function loadEditor(url) {
		$(window).bind('message', function(event) {
			if (event.originalEvent.source == editor) {
				var msg = event.originalEvent.data;
				if (msg == 'OK\n\nGET line.column') {
					location.href = url;
				} else if (msg.indexOf('OK\n\nGET line.column\n\n') == 0) {
					location.href = url + '#' + msg.substring(msg.lastIndexOf('\n\n') + 2);
				}
			}
		});
		editor.postMessage('GET line.column', '*');
	}
});

})(jQuery);


