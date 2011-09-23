// dialog.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

if (!window.calli) {
	window.calli = {};
}

window.calli.dialogClose = function(iframe) {
	$(iframe.frameElement).dialog('close');
}

window.calli.dialog = function(url, title, options) {
	var width = 450;
	var height = 500;
	height += 50; // title bar
	if (options.buttons) {
		height += 50; // button bar
	}
	var innerHeight = window.innerHeight || document.documentElement.clientHeight;
	while (height >= 200 && width >= 200 && (height > innerHeight || width > document.documentElement.clientWidth)) {
		height -= 100;
		width -= 100;
	}
	var settings = jQuery.extend({
		title: title,
		autoOpen: false,
		modal: false,
		draggable: true,
		resizable: false,
		autoResize: true,
		position: ['center', 'center'],
		width: width,
		height: height
	}, options);
	var iframe = $("<iframe></iframe>");
	iframe.attr('src', url);
	iframe.addClass('dialog');
	iframe.dialog(settings);
	var requestedHeight = height;
	var requestedWidth = width;
	var setDialogHeight = function(height) {
		iframe.dialog("option", "height", height);
		var fheight = height;
		iframe.siblings().each(function(){
			fheight -= $(this).outerHeight(true);
		});
		iframe.height(fheight);
	};
	var handle = function(event) {
		if (event.originalEvent.source == iframe[0].contentWindow) {
			var data = event.originalEvent.data;
			if (data.indexOf('PUT height\n\n') == 0) {
				var height = parseInt(data.substring(data.indexOf('\n\n') + 2));
				var dheight = height + iframe.dialog("option", "height") - iframe.height();
				requestedHeight = Math.max(requestedHeight, dheight);
				var innerHeight = window.innerHeight || document.documentElement.clientHeight;
				if (dheight <= innerHeight) {
					if (dheight > iframe.dialog("option", "height")) {
						setDialogHeight(dheight);
					}
				} else if (height > innerHeight && window.parent) {
					parent.postMessage('PUT height\n\n' + dheight, '*');
				} else {
					var position = iframe.dialog("option", "position");
					position[1] = 'top';
					iframe.dialog("option", "position", position);
					setDialogHeight(innerHeight);
				}
				iframe[0].contentWindow.postMessage('OK\n\PUT height', '*');
			} else if (data.indexOf('PUT width\n\n') == 0) {
				var width = parseInt(data.substring(data.indexOf('\n\n') + 2));
				requestedWidth = Math.max(requestedWidth, width);
				if (width <= document.documentElement.clientWidth) {
					if (width > iframe.dialog("option", "width")) {
						iframe.dialog("option", "width", width);
					}
				} else if (width > document.documentElement.clientWidth && window.parent) {
					parent.postMessage('PUT width\n\n' + width, '*');
				} else {
					var position = iframe.dialog("option", "position");
					position[0] = 'left';
					iframe.dialog("option", "position", position);
					iframe.dialog("option", "width", document.documentElement.clientWidth);
				}
				iframe[0].contentWindow.postMessage('OK\n\nPUT width', '*');
			} else if (typeof options.onmessage == 'function') {
				if (!event.source) {
					event.source = event.originalEvent.source;
				}
				if (!event.data) {
					event.data = event.originalEvent.data;
				}
				options.onmessage(event);
			}
		}
	};
	$(window).bind('message', handle);
	$(window).bind('resize', function(){
		var clientWidth = document.documentElement.clientWidth;
		if (requestedWidth > clientWidth) {
			var position = iframe.dialog("option", "position");
			position[0] = 'left';
			iframe.dialog("option", "position", position);
		}
		iframe.dialog("option", "width", Math.min(clientWidth, requestedWidth));
		var innerHeight = window.innerHeight || document.documentElement.clientHeight;
		if (requestedHeight > innerHeight) {
			var position = iframe.dialog("option", "position");
			position[1] = 'top';
			iframe.dialog("option", "position", position);
		}
		setDialogHeight(Math.min(innerHeight, requestedHeight));
	});
	iframe.one('load', function(){
		setTimeout(function() {
			iframe.dialog("option", "position", ['center', 'center']);
		}, 0);
	});
	iframe.bind("dialogclose", function(event, ui) {
		$(window).unbind('message', handle);
		iframe.remove();
		iframe.parent().remove();
		if (typeof options.onclose == 'function') {
			options.onclose();
		}
	});
	iframe.dialog("open");
	iframe.css('width', '100%');
	return iframe[0].contentWindow;
}

})(jQuery);

