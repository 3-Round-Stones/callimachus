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
	function getPageLocationURL() {
		// window.location.href needlessly decodes URI-encoded characters in the URI path
		// https://bugs.webkit.org/show_bug.cgi?id=30225
		var path = location.pathname;
		if (path.match(/#/))
			return location.href.replace(path, path.replace('#', '%23'));
		return location.href;
	}
	$('#form[data-type]').submit(function(event) {
		var form = this;
		event.preventDefault();
		calli.saveas($(this).attr('data-name'), form, function(parent, label, ns, local) {
			var header = 'POST create'
				+ '\nAction: ' + form.action
				+ '\nLocation: ' + ns + local.replace(/\+/g,'-').toLowerCase()
				+ '\nContent-Type: ' + $(form).attr('data-type');
			if ($('#cache').val()) {
				header += '\nCache-Control: ' + $('#cache').val();
			}
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
		});
		return false;
	});
});

