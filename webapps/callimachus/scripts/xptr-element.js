// xptr-element.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

if (!window.calli) {
	window.calli = {};
}

window.calli.xptr = function (element) {
	var result = [];
	result.unshift($(element).parent().children().index(element) + 1);
	$(element).parents().each(function(i) {
		result.unshift($(this).parent().children().index(this) + 1);
	});
	result.unshift('');
	return result.join('/');
};

})(jQuery);

