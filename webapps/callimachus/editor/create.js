// create.js

(function($, jQuery){
	$(window).bind('message', function(event) {
		if (event.originalEvent.source == $('#iframe')[0].contentWindow && event.originalEvent.data == 'CONNECT calliEditorLoaded') {
			var template = $('#template').text() || $('#template').html();
			if (window.location.hash.indexOf('#!') == 0) {
				jQuery.ajax({type: 'GET', url: window.location.hash.substring(2), complete: function(xhr) {
					if (xhr.status == 200 || xhr.status == 304) {
						var text = xhr.responseText;
						$('#iframe')[0].contentWindow.postMessage('POST template\n\n' + text, '*');
					}
				}});
			} else if (template) {
				$('#iframe')[0].contentWindow.postMessage('POST template\n\n' + template, '*');
			}
		}
	});
	$(window).bind('message', function(event) {
		if (event.originalEvent.source == $('#iframe')[0].contentWindow) {
			var msg = event.originalEvent.data;
			if (msg.indexOf('ERROR ') == 0) {
				var message = msg.substring('ERROR '.length, msg.indexOf('\n\n'));
				var stack = msg.substring(msg.indexOf('\n\n', msg.indexOf('\n\n') + 2) + 2);
				calli.error(message, stack);
			} else if (msg.indexOf('ERROR') == 0) {
				calli.error(msg.substring(msg.indexOf('\n\n', msg.indexOf('\n\n') + 2) + 2));
			}
		}
	});
	$(document).bind('calliOpenDialog', function(event) {
		if (!event.isDefaultPrevented()) {
			$('#iframe')[0].contentWindow.postMessage('PUT disabled\n\ntrue', '*');
		}
	});
	$(document).bind('calliCloseDialog', function(event) {
		if (!event.isDefaultPrevented()) {
			$('#iframe')[0].contentWindow.postMessage('PUT disabled\n\nfalse', '*');
		}
	});
})(jQuery, jQuery);

jQuery(function($){
	$('form[enctype]').submit(function(event) {
		var form = this;
		var about = $(form).attr('about');
		if (!about || about.indexOf(':') < 0 && about.indexOf('/') != 0 && about.indexOf('?') != 0)
			return true; // about attribute not set yet
		event.preventDefault();
		var header = 'POST create'
			+ '\nAction: ' + form.action
			+ '\nContent-Type: ' + $(form).attr('enctype');
		$(window).bind('message', function(event) {
			if (event.originalEvent.source == $('#iframe')[0].contentWindow) {
				var msg = event.originalEvent.data;
				if (msg.indexOf('OK\n\n' + header + '\n\n') == 0) {
					var url = msg.substring(msg.lastIndexOf('\n\n') + 2);
					location.replace(url + '?view');
				}
			}
		});
		$('#iframe')[0].contentWindow.postMessage(header, '*');
		return false;
	});
});

