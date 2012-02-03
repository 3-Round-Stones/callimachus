// edit.js

(function($){
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
	$('form[enctype]').submit(function(event) {
		if ($(this).attr('about'))
			return true; // saveas has been called
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
						loadEditor(url + '?edit');
				}
			}
		});
		$('#iframe')[0].contentWindow.postMessage(header, '*');
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

