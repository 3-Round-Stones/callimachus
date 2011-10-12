// remove-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

calli.removeResource = function(node) {
	var parents = $(node).parents();
	for (var i=0; i<parents.length; i++) {
		if ($(parents[i]).is('[about],[resource],[href],[src]')) {
			$(parents[i]).remove();
			return false;
		}
	}
	return true;
};

})(jQuery);

