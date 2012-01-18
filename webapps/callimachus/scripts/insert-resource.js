// insert-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

calli.insertResource = function(event) {
	event = calli.fixEvent(event);
	var target = event.target;
	event.preventDefault();
	var text = event.dataTransfer.getData('URL');
	if (!text) {
		text = event.dataTransfer.getData("Text");
	}
	var errorMessage = event.errorMessage ? event.errorMessage : "Invalid Relationship";
	select(target, '[data-construct]').each(function(i, script) {
		window.calli.listResourceIRIs(text).each(function() {
			addSetItem(this, $(script), errorMessage);
		});
	});
	return false;
};

function select(node, selector) {
	var set = $(node).add($(node).parents(selector)).filter(selector);
	if (set.length)
		return set;
	set = $(node).find(selector);
	if (set.length)
		return set;
	set = $(node).parents('[dropzone]').find(selector);
	return set;
}

function addSetItem(uri, script, errorMessage) {
	var position = 0;
	var url = script.attr("data-construct").replace("{about}", encodeURIComponent(uri));
	jQuery.get(url, function(data) {
		var input = data ? $(data).children("[data-var-about],[data-var-resource]") : data;
		if (input && input.length) {
			if (position > 0 && script.children().length >= position) {
				script.children()[position - 1].before(input);
			} else {
				script.append(input);
			}
			var de = jQuery.Event('calliLinked');
			de.location = uri;
			$(input).trigger(de);
		} else if (errorMessage) {
			script.trigger("calliError", errorMessage);
		}
	});
}

})(jQuery);

