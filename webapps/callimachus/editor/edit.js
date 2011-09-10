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
		var name = prompt('Save as...', $(this).attr('data-label'));
		if (name) {
			var local = encodeURI(name).replace(/%20/g,'-').toLowerCase();
			var header = 'POST create'
				+ '\nAction: ./?create=' + $(this).attr('data-create')
				+ '\nLocation: ' + local + $(this).attr('data-suffix');
			$(window).bind('message', function(event) {
				if (event.originalEvent.source == $('#iframe')[0].contentWindow) {
					var msg = event.originalEvent.data;
					if (msg.indexOf('OK\n\n' + header + '\n\n') == 0) {
						var url = msg.substring(msg.lastIndexOf('\n\n')).match(/Location:\s*(\S+)/)[1]
						loadEditor(url + '?edit');
					}
				}
			});
			$('#iframe')[0].contentWindow.postMessage(header, '*');
		}
		return false;
	});
	function loadEditor(url) {
		$(window).bind('message', function(event) {
			if (event.originalEvent.source == $('#iframe')[0].contentWindow) {
				var msg = event.originalEvent.data;
				if (msg.indexOf('OK\n\nGET line.column\n\n') == 0) {
					location.href = url + '#' + msg.substring(msg.indexOf('\n\n', 4) + 2);
				}
			}
		});
		$('#iframe')[0].contentWindow.postMessage('GET line.column', '*');
	}
});

