// data-more.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	var form = $("form[about]");
	createAddRelButtons(form);
});

$(document).bind("DOMNodeInserted", function (event) {
	createAddRelButtons(event.target);
});

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function createAddRelButtons(form) {
	select(form, "[data-more][rel]").each(function() {
		var parent = $(this);
		var property = parent.attr("rel");
		var editable = parent.is("*[contenteditable]") || parent.parents("*[contenteditable]").size();
		var minOccurs = parent.attr("data-min-cardinality");
		var maxOccurs = parent.attr("data-max-cardinality");
		minOccurs = minOccurs ? minOccurs : parent.attr("data-cardinality");
		maxOccurs = maxOccurs ? maxOccurs : parent.attr("data-cardinality");
		var count = parent.children().size();
		var add = false;
		parent.children().each(initSetElement);
		if (!editable && (!maxOccurs || !minOccurs || parseInt(minOccurs) != parseInt(maxOccurs))) {
			add = $('<button type="button"/>');
			add.addClass("add");
			add.text("»");
			add.click(function(){
				jQuery.get(parent.attr("data-more"), function(data) {
					var input = $(data);
					add.before(input);
					input.each(initSetElement);
					updateButtonState(parent);
				});
			});
			if (this.tagName.toLowerCase() == "table") {
				var cell = $("<td/>");
				cell.append(add);
				var row = $("<tr/>");
				row.append(cell);
				var tbody = $("<tbody/>");
				tbody.append(row);
				parent.append(tbody);
			} else if (this.tagName.toLowerCase() == "tbody") {
				var cell = $("<td/>");
				cell.append(add);
				var row = $("<tr/>");
				row.append(cell);
				parent.append(row);
			} else if (this.tagName.toLowerCase() == "tr") {
				var cell = $("<td/>");
				cell.append(add);
				parent.append(cell);
			} else {
				parent.append(add);
			}
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
					input.each(initSetElement);
					updateButtonState(parent);
				});
			}
		}
		updateButtonState(parent);
	});
}

function updateButtonState(context) {
	var minOccurs = context.attr("data-min-cardinality");
	var maxOccurs = context.attr("data-max-cardinality");
	minOccurs = minOccurs ? minOccurs : context.attr("data-cardinality");
	maxOccurs = maxOccurs ? maxOccurs : context.attr("data-cardinality");
	var add = context.children("button[class='add']");
	var buttons = context.children("[id]").map(function(){
		return context.find("button[class='remove'][data-for='" + $(this).attr("id") + "']").get(0);
	});
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

var counter = 0;

function initSetElement() {
	var row = $(this);
	if (!row.parents("*[contenteditable]").size()) {
		var remove = $('<button type="button"/>');
		remove.addClass("remove");
		if (!row.attr("id")) {
			row.attr("id", "element" + (++counter));
		}
		remove.attr("data-for", row.attr("id"));
		remove.text("×");
		remove.click(function(){
			var list = row.parent();
			row.remove();
			remove.remove();
			updateButtonState(row);
		})
		if (row.is(":input")) {
			row.after(remove);
		} else if (this.tagName.toLowerCase() == "tr") {
			var cell = $("<td/>");
			cell.append(remove);
			row.append(cell);
		} else {
			row.append(remove);
		}
	}
}

})(jQuery);

