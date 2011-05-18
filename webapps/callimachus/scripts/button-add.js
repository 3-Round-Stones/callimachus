// button-add.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	bindAddButtons($("button.add[data-more]"));
});

$(document).bind("DOMNodeInserted", function (event) {
	bindAddButtons($(event.target).find("button.add[data-more]").andSelf().filter("button.add[data-more]"));
});

function bindAddButtons(buttons) {
	buttons.click(function() {
		var button = $(this);
		jQuery.get(button.attr("data-more"), function(data) {
			button.before($(data).clone());
		}, 'text');
	});
}

})(jQuery);

