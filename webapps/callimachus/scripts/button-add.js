// button-add.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	bindAddButtons($("button.add"));
});

$(document).bind("DOMNodeInserted", function (event) {
	bindAddButtons($(event.target).find("button.add").andSelf().filter("button.add"));
});

function bindAddButtons(buttons) {
	buttons.click(function() {
		var button = $(this);
		var more = button.parent().attr("data-more");
		if (more) {
			jQuery.get(more, function(data) {
				var input = $(data);
				button.before(input);
			});
		}
	});
}

})(jQuery);

