// dropzone.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function() {
	initDropArea($("[data-construct]"));
});

$(document).bind('DOMNodeInserted', function (event) {
	initDropArea($(event.target).find("[data-construct]").andSelf().filter("[data-construct]"));
});

function initDropArea(construct) {
	var dropzone = construct.add(construct.parents()).filter('[dropzone]');
	dropzone.bind('dragenter dragover', function(event) {
		if (!$(this).hasClass("drag-over")) {
			$(this).addClass("drag-over");
		}
		event.preventDefault();
		return false;
	});
	dropzone.bind('dragleave', function(event) {
		$(this).removeClass("drag-over");
		event.preventDefault();
		return false;
	});
	dropzone.bind('drop', function(event) {
		var target = event.target;
		event.preventDefault();
		$(this).removeClass("drag-over");
		var data = event.originalEvent ? event.originalEvent.dataTransfer : event.dataTransfer;
		var text = data.getData('URL');
		if (!text) {
			text = data.getData("Text");
		}
		var de = jQuery.Event('calliLink');
		de.location = text;
		de.errorMessage = "Invalid Relationship";
		$(target).trigger(de);
		return false;
	});
	dropzone.bind('calliLink', function(event) {
		select(event.target, '[data-construct]').each(function(i, script) {
			window.calli.listResourceIRIs(event.location).each(function() {
				addSetItem(this, $(script), event.errorMessage);
				event.preventDefault();
			});
		});
	});
}

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
	var m = script.attr("data-construct").match(/\belement=\/(\d+\/)*(\d+)\b/);
	if (m) {
		position = parseInt(m[0][2]);
	}
	jQuery.get(url, function(data) {
		var input = data ? $(data).children("[about],[resource]") : data;
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

