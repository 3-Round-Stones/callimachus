/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var rdfnil = "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil";

$(document).ready(function () {
	var form = $("form[about]");
	var lists = $("[data-member]", form);
	lists.each(function() {
		var list = $(this);
		var members = $("[rel='rdf:first'],[rel='rdf:rest']", list);
		members.each(function() {
			var row = $(this);
			if (!row.attr("about") && row.parent().attr("resource")) {
				row.attr("about", row.parent().attr("resource"));
			}
			if (!row.attr("resource") && row.children().attr("about")) {
				row.attr("resource", row.children().attr("about"));
			}
		})
		$("[rel='rdf:first']").each(initListElement);
		list.append(members);
		list.sortable({
				items: "[rel='rdf:first']",
				update: function(event, ui) { updateList(list) }
		});
		var dragover = function(event) {
			if (!list.hasClass("drag-over")) {
				list.addClass("drag-over");
			}
			return stopPropagation(event);
		}
		var dragleave = function(event) {
			list.removeClass("drag-over");
			return stopPropagation(event);
		}
		if (this.addEventListener) {
			this.addEventListener("dragenter", dragover, false);
			this.addEventListener("dragover", dragover, false);
			this.addEventListener("dragleave", dragleave, false);
			this.addEventListener("drop", pasteListURL(dragleave), false);
		} else if (this.attachEvent) {
			this.attachEvent("ondragenter", dragover);
			this.attachEvent("ondragover", dragover);
			this.attachEvent("ondragleave", dragleave);
			this.attachEvent("ondrop", pasteListURL(dragleave));
		}
	})
});

function initListElement() {
	var row = $(this);
	var remove = $('<button type="button"/>');
	remove.addClass("remove");
	remove.text("×");
	remove.click(function(){
		var list = row.parent();
		var rest = list.children("[about='" + row.attr("about") + "'][rel='rdf:rest']");
		row.remove();
		rest.remove();
		updateList(list);
	});
	row.append(remove);
}

function stopPropagation(event) {
	if (event.preventDefault) {
		event.preventDefault();
	}
	return false;
}

function pasteListURL(callback) {
	return function(event) {
		var target = event.target ? event.target : event.srcElement;
		if (event.preventDefault) {
			event.preventDefault();
		}
		var data = event.originalEvent ? event.originalEvent.dataTransfer : event.dataTransfer;
		if (!data) {
			data = event.clipboardData;
		}
		if (!data) {
			data = window.clipboardData;
		}
		var uris = window.calli.listResourceIRIs(data.getData('URL'));
		if (!uris.size()) {
			uris = window.calli.listResourceIRIs(data.getData("Text"));
		}
		var selector = "[data-member]";
		var script = $(target);
		if (!script.is(selector)) {
			$(target).parents().each(function() {
				var s = $(this).children(selector);
				if (s.size()) {
					script = s;
				}
			});
		}
		uris.each(function() {
			addListItem(this, script);
		});
		return callback(event);
	}
}

function addListItem(uri, list) {
	var url = list.attr("data-member").replace("{about}", encodeURIComponent(uri));
	jQuery.get(url, function(data) {
		var input = !data || typeof data == 'string' && data.match(/^\s*$/) ? null : $(data);
		if (input && input.size()) {
			var first = $("<div/>");
			first.attr("about", '[' + $.rdf.blank('[]').value + ']');
			first.attr("rel", "rdf:first");
			first.attr("resource", uri);
			first.append(input);
			list.append(first);
			input.each(initListElement);
			updateList(list);
			if (script.attr("data-dialog")) {
				$('#' + script.attr("data-dialog")).dialog('close');
			}
		} else {
			list.parents("form").trigger("calliError", "Invalid Relationship");
		}
	});
}

function updateList(list) {
	var elements = list.children("[rel='rdf:first']");
	if (elements.size()) {
		list.attr("resource", $(elements.get(0)).attr("about"));
	} else {
		list.attr("resource", rdfnil);
	}
	elements.each(function(i){
		var about = $(this).attr("about");
		if (about) {
			var rest = list.children("[about='" + about + "'][rel='rdf:rest']");
			if (rest.size() && i + 1 < elements.size()) {
				rest.attr("resource", $(elements.get(i + 1)).attr("about"));
			} else if (rest.size()) {
				rest.attr("resource", rdfnil);
			} else {
				var rest = $("<div/>");
				rest.attr("about", about);
				rest.attr("rel", "rdf:rest");
				if (i + 1 < elements.size()) {
					rest.attr("resource", $(elements.get(i + 1)).attr("about"));
				} else {
					rest.attr("resource", rdfnil);
				} 
				list.append(rest);
			}
		}
	});
}

})(jQuery);

