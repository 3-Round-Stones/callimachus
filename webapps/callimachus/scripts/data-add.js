// data-add.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	bindAddButtons($("button.add[data-add]"));
});

$(document).bind("DOMNodeInserted", function (event) {
	bindAddButtons($(event.target).find("button.add[data-add]").andSelf().filter("button.add[data-add]"));
});

function bindAddButtons(buttons) {
	buttons.prepend('<span class="ui-icon ui-icon-plusthick" style="display:inline-block;vertical-align:text-bottom"></span>');
	buttons.click(function() {
		var button = $(this);
		jQuery.get(button.attr("data-add"), function(data) {
			var clone = $(data).clone();
			button.before(clone);
			clone.find(':input').andSelf().filter(':input').focus();
		}, 'text');
	});
}

})(jQuery);

