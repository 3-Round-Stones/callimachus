// remove-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

calli.removeResource = function(event) {
	var node = event.target ? event.target : event.srcElement ? event.srcElement : event;
	if (node.nodeType == 3) node = node.parentNode; // defeat Safari bug
	var parents = $(node).add($(node).parents());
	for (var i=0; i<parents.length; i++) {
		if ($(parents[i]).is('[data-var-about],[data-var-resource],[data-var-href],[data-var-src],[typeof]')) {
			$(parents[i]).remove();
			return false;
		}
	}
	return true;
};

})(jQuery);

