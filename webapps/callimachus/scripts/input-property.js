// input-property.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	var form = $("form[about]");
	createAddPropertyButtons(form);
	$(":input[property]", form).each(initInputElement);
});

$("form[about]").live("DOMNodeInserted", function (event) {
	createAddPropertyButtons(event.target);
	select(event.target, ":input[property]").each(initInputElement);
});

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function createAddPropertyButtons(form) {
	select(form, "[data-more]").each(function() {
		var parent = $(this);
		var minOccurs = parent.attr("data-min-cardinality");
		var maxOccurs = parent.attr("data-max-cardinality");
		minOccurs = minOccurs ? minOccurs : parent.attr("data-cardinality");
		maxOccurs = maxOccurs ? maxOccurs : parent.attr("data-cardinality");
		var count = parent.children("[property]").size();
		var add = false;
		if (!maxOccurs || !minOccurs || parseInt(minOccurs) != parseInt(maxOccurs)) {
			add = $('<button type="button"/>');
			add.addClass("add");
			add.text("»");
			add.click(function(){
				jQuery.get(parent.attr("data-more"), function(data) {
					var input = $(data);
					add.before(input);
					input.find().andSelf().filter(":input:first").focus();
					updateButtonState(parent);
				});
			});
			parent.append(add);
		}
		if (minOccurs) {
			var n = parseInt(minOccurs) - count;
			for (var i=0; i<n; i++) {
				jQuery.get(parent.attr("data-more"), function(data) {
					var input = $(data);
					if (add) {
						add.before(input);
					} else {
						parent.append(input);
					}
					updateButtonState(parent);
				});
			}
		}
		updateButtonState(parent);
	});
}

function initInputElement() {
	var input = $(this);
	var property = input.attr("property");
	input.change(function(){
		var value = $(this).val();
		if (value) {
			$(this).attr("property", property);
			$(this).attr("content", value);
		} else {
			$(this).removeAttr("property");
			$(this).removeAttr("content");
		}
	});
	input.change();
	var parent = input.parent();
	var remove = $('<button type="button"/>');
	remove.addClass("remove");
	remove.text("×");
	remove.click(function(){
		input.remove();
		remove.remove();
		updateButtonState(parent);
	});
	input.after(remove);
	updateButtonState(parent);
}

function updateButtonState(context) {
	var minOccurs = context.attr("data-min-cardinality");
	var maxOccurs = context.attr("data-max-cardinality");
	minOccurs = minOccurs ? minOccurs : context.attr("data-cardinality");
	maxOccurs = maxOccurs ? maxOccurs : context.attr("data-cardinality");
	var add = context.children("button[class='add']");
	var buttons;
	if (context.attr("rel")) {
		buttons = context.children().children("button[class='remove']");
	} else {
		buttons = context.children("button[class='remove']");
	}
	if (buttons.size() <= minOccurs) {
		buttons.hide();
	} else {
		buttons.show();
	}
	if (buttons.size() >= maxOccurs) {
		add.hide();
	} else {
		add.show();
	}
}

})(jQuery);

