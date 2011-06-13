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
	var bheight = button.outerHeight(true);
	var bwidth = button.outerWidth(true);
	$(this).find(':not(button)').each(function() {
		var vmargin = 0;
		var hmargin = 0;
		var width = $(this).outerWidth(true);
		var height = $(this).outerHeight(true);
		if (width < bwidth) {
			vmargin = height / 2 - bheight / 2;
			hmargin = width / 2 - bwidth / 2;
		} else if (height >= bheight * 2) {
			vmargin = bheight * 0.5;
			hmargin = bheight * 0.5;
		} else {
			vmargin = height / 2 - bheight / 2;
			hmargin = 0;
		}
		var offset = $(this).offset();
		if (!top || offset.top > top) {
			top = offset.top + vmargin;
		}
		var right = offset.left + width;
		if (!left || right > left) {
			left = right - bwidth - hmargin;
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

