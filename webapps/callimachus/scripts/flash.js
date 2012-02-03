// flash.js

(function($){

$(document).error(function(event) {
	if (event.message && !event.isDefaultPrevented()) {
		flash(event.message, event.stack);
	}
});

function flash(message, stack) {
	var msg = $("#flash");
	if (msg.size()) {
		var widget = $('<div/>');
		widget.addClass("ui-state-error ui-corner-all error");
		widget.css("padding", "1ex");
		widget.css("margin", "1ex");
		var p = $('<div/>');
		var icon = $('<span/>');
		icon.addClass("ui-icon ui-icon-alert");
		icon.css("margin-right", "0.3em");
		icon.css("float", "left");
		var close = $('<span/>');
		close.addClass("ui-icon ui-icon-close");
		var button = $('<span/>');
		button.css("display", "block");
		button.css("float", "right");
		button.click(function() {
			widget.remove();
		});
		button.append(close);
		p.append(icon);
		p.append(button);
		p.append(message);
		widget.append(p);
		if (stack) {
			var pre = $("<pre/>");
			pre.append(stack);
			pre.hide();
			widget.append(pre);
			p.click(function() {
				pre.toggle();
			});
		}
		msg.append(widget);
		scroll(0,0);
	} else {
		throw message;
	}
}

})(window.jQuery);

