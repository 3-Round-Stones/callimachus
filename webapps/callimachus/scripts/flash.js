// flash.js

(function($){

$(document).error(function(event) {
	var status = event.message;
	var detail = event.data;
	if (status && !event.isDefaultPrevented()) {
		var msg = $("#flash");
		if (msg.size()) {
			var widget = $('<div/>');
			widget.addClass("ui-state-error ui-corner-all");
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
			appendTo(status, p);
			widget.append(p);
			if (detail) {
				var pre = $("<pre/>");
				appendTo(detail, pre);
				pre.hide();
				widget.append(pre);
				p.click(function() {
					pre.toggle();
				});
			}
			msg.append(widget);
			scroll(0,0);
		} else {
			alert(status);
		}
	}
});

function appendTo(obj, node) {
	if (typeof obj == 'string') {
		node.append(document.createTextNode(obj));
	} else if (obj.nodeType) {
		node.append(obj);
	} else if (typeof obj.toSource == 'function') {
		node.append(document.createTextNode(obj.toSource()));
	} else if (obj.message) {
		node.append(document.createTextNode(obj.message));
	} else {
		node.append(document.createTextNode(obj));
	}
}

})(window.jQuery);

