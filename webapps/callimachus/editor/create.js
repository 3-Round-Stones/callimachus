// create.js

(function($){
	$(window).bind('message', function(event) {
		if (event.originalEvent.source == $('#iframe')[0].contentWindow && event.originalEvent.data == 'CONNECT calliEditorLoaded') {
			var template = $('#template').text();
			if (template) {
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
	$('#form[data-type]').submit(function(event) {
		event.preventDefault();
		var name = prompt('Save as...', $(this).attr('data-label'));
		if (name) {
			var local = encodeURI(name).replace(/%20/g,'-').toLowerCase();
			var header = 'POST create'
				+ '\nAction: ' + location.href
				+ '\nLocation: ' + location.pathname + local + $(this).attr('data-suffix')
				+ '\nContent-Type: ' + $(this).attr('data-type');
			if ($('#cache').val()) {
				header += '\nCache-Control: ' + $('#cache').val();
			}
			$(window).bind('message', function(event) {
				if (event.originalEvent.source == $('#iframe')[0].contentWindow) {
					var msg = event.originalEvent.data;
					if (msg.indexOf('OK\n\n' + header + '\n\n') == 0) {
						var url = msg.substring(msg.lastIndexOf('\n\n')).match(/Location:\s*(\S+)/)[1]
						location.replace(url + '?view');
					}
				}
			});
			$('#iframe')[0].contentWindow.postMessage(header, '*');
		}
		return false;
	});
});

