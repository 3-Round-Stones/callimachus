// edit.js

(function($){
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
	function onhashchange() {
		var path = location.pathname;
		if (location.hash) {
			path += location.hash;
		}
		var src = $('#iframe')[0].src;
		if (src.indexOf('!') > 0) {
			src = src.substring(0, src.indexOf('!'));
		}
		if (src.indexOf('#') < 0) {
			src = src + '#';
		}
		$('#iframe')[0].src = src + '!' + path;
	}
	$(window).bind('hashchange', onhashchange);
	onhashchange();
});

jQuery(function($){
	$('#form').submit(function(event) {
		event.preventDefault();
		$(window).bind('message', function(event) {
			if (event.originalEvent.source == $('#iframe')[0].contentWindow) {
				var msg = event.originalEvent.data;
				if (msg == 'OK\n\nPOST save') {
					location.replace('?view');
				}
			}
		});
		$('#iframe')[0].contentWindow.postMessage('POST save', '*');
		return false;
	});
	$('#saveas[data-create]').click(function(event) {
		event.preventDefault();
		var button = $(this);
		calli.saveas(button.attr('data-name'), function(parent, label, ns, local) {
			var header = 'POST create'
				+ '\nAction: ' + parent + '?create=' + button.attr('data-create')
				+ '\nLocation: ' + ns + local.replace(/\+/g,'-').toLowerCase();
			$(window).bind('message', function(event) {
				if (event.originalEvent.source == $('#iframe')[0].contentWindow) {
					var msg = event.originalEvent.data;
					if (msg.indexOf('OK\n\n' + header + '\n\n') == 0) {
						var url = msg.substring(msg.lastIndexOf('\n\n') + 2);
						loadEditor(url + '?edit');
					}
				}
			});
			$('#iframe')[0].contentWindow.postMessage(header, '*');
		});
		return false;
	});
	function loadEditor(url) {
		$(window).bind('message', function(event) {
			if (event.originalEvent.source == $('#iframe')[0].contentWindow) {
				var msg = event.originalEvent.data;
				if (msg == 'OK\n\nGET line.column') {
					location.href = url;
				} else if (msg.indexOf('OK\n\nGET line.column\n\n') == 0) {
					location.href = url + '#' + msg.substring(msg.lastIndexOf('\n\n') + 2);
				}
			}
		});
		$('#iframe')[0].contentWindow.postMessage('GET line.column', '*');
	}
});

