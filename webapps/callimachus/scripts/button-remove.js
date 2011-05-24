// button-remove.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	bindRemoveButtons($("button.remove"));
});

$(document).bind("DOMNodeInserted", function (event) {
	bindRemoveButtons($(event.target).find("button.remove").andSelf().filter("button.remove"));
});

function showButton() {
	var button = $(this).children('button.remove');
	var top = 0;
	var left = 0;
	$(this).find(':not(button)').each(function() {
		var margin = 0;
		if ($(this).height() >= button.height() * 2) {
			margin = button.height() * 0.5;
		} else {
			margin = $(this).height() / 2 - button.height() / 2;
		}
		var offset = $(this).offset();
		if (!top || offset.top > top) {
			top = offset.top + margin;
		}
		var right = offset.left + $(this).width();
		if (!left || right > left) {
			left = right - button.outerWidth(true) - margin;
		}
	});
	button.css('display', "inline-block");
	button.css('position', 'absolute');
	button.css('top', top);
	button.css('left', left);
}

function bindRemoveButtons(buttons) {
	buttons.click(function() {
		var button = $(this);
		var parent = button.parent();
		if (parent.is("td,th")) {
			parent.parent().remove();
		} else {
			parent.remove();
		}
	});
	buttons.css('display', "none");
	buttons.prepend('<span class="ui-icon ui-icon-closethick" style="display:inline-block;vertical-align:text-bottom"></span>');
	buttons.parent().hover(showButton, function() {
		$(this).children('button.remove').fadeOut();
	});
	buttons.parent().keydown(function(event) {
		$(this).children('button.remove').fadeOut();
		return true;
	});
}

})(jQuery);

