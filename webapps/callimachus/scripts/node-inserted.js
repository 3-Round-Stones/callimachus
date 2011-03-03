// node-inserted.js

jQuery(function($){

function isIE() {
	var msie = true;
	function notmsie() {
		msie = false;
	}
	$(document).bind("DOMNodeInserted", notmsie);
	var div = $("<div/>");
	div.appendTo($("body"));
	div.remove();
	return msie;
}

if (isIE()) {
	var _domManip = $.fn.domManip;
	var _remove = $.fn.remove;

	// 'append', 'prepend', 'after', 'before'
	$.fn.domManip = function(args, table, callback) {
		return _domManip.call(this, args, table, function(fragment) {
			var el = jQuery(fragment).children();
			var ret = callback.call(this, fragment);
			el.trigger("DOMNodeInserted");
			el.trigger("DOMSubtreeModified");
			return ret;
		});
	};
	$.fn.remove = function() {
		var ret = _remove.apply(this, arguments);
		var el = jQuery("*", this).add([this]);
		el.trigger("DOMNodeRemoved");
		el.trigger("DOMSubtreeModified");
		return ret;
	};
}

});

