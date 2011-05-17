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
}

})(jQuery);

