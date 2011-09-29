// create.js

(function($){
	$(window).bind('message', function(event) {
		if (event.originalEvent.source == $('#iframe')[0].contentWindow && event.originalEvent.data == 'CONNECT calliEditorLoaded') {
			var template = $('#template').text();
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
				var status = msg.substring('ERROR '.length, msg.indexOf('\n\n'));
				var error = msg.substring(msg.indexOf('\n\n', msg.indexOf('\n\n') + 2) + 2);
				$(document).trigger("calliError", status, error);
			} else if (msg.indexOf('ERROR') == 0) {
				var error = msg.substring(msg.indexOf('\n\n', msg.indexOf('\n\n') + 2) + 2);
				$(document).trigger("calliError", error);
			}
		}
	});
})(jQuery);

jQuery(function($){
	$('form[enctype]').submit(function(event) {
		var form = this;
		var about = $(form).attr('about');
		if (!about || about.indexOf(':') < 0 && about.indexOf('/') != 0 && about.indexOf('?') != 0)
			return true; // about attribute not set yet
		event.preventDefault();
		var header = 'POST create'
			+ '\nAction: ' + form.action
			+ '\nLocation: ' + about
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

