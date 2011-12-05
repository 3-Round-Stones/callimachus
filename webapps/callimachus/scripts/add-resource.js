// add-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

calli.addTemplate = calli.addResource = function(event) {
	var node = event.target ? event.target : event.srcElement ? event.srcElement : event;
	if (node.nodeType == 3) node = node.parentNode; // defeat Safari bug
	var rel = $(node).add($(node).parents()).filter('[data-add]');
	var add = rel.attr("data-add");
	if (!add)
		return true;
	jQuery.get(add, function(data) {
		var clone = $(data).clone();
		var child = clone.children("[about],[typeof],[typeof=''],[resource],[property]");
		if ($(node).attr("data-add")) {
			$(node).append(child);
		} else {
			$(node).before(child);
		}
		child.find(':input').andSelf().filter(':input:first').focus();
	}, 'text');
	return false;
};

})(jQuery);

