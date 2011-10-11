// button-remove.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	bindRemoveButtons($("button.remove,a.remove"));
});

$(document).bind("DOMNodeInserted", function (event) {
	bindRemoveButtons($(event.target).find("button.remove,a.remove").andSelf().filter("button.remove,a.remove"));
});

function bindRemoveButtons(buttons) {
	buttons.click(function() {
		var button = $(this);
		var parent = button.parents('[about],[resource],[href],[src]')[0];
		if (parent) {
			$(parent).remove();
		}
	});
	buttons.append('<span class="ui-icon ui-icon-closethick" style="display:inline-block;vertical-align:middle"></span>');
}

})(jQuery);

