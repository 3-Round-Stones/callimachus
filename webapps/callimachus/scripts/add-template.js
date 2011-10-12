// add-template.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

calli.addTemplate = function(node) {
	var button = $(node).add($(node).parents()).filter('[data-add]');
	var add = button.attr("data-add");
	if (!add)
		return true;
	jQuery.get(add, function(data) {
		var clone = $(data).clone();
		$(node).before(clone.children("[about],[typeof],[typeof=''],[resource],[property]"));
		clone.find(':input').andSelf().filter(':input').focus();
	}, 'text');
	return false;
};

})(jQuery);

