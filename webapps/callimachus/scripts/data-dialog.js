// data-dialog.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function() {
	initDialogButton($("button[data-dialog]"));
});

$(document).bind('DOMNodeInserted', function (event) {
	initDialogButton($(event.target).find("button[data-dialog]").andSelf().filter("button[data-dialog]"));
});

var iframe_counter = 0;

function initDialogButton(buttons) {
	buttons.prepend('<span class="ui-icon ui-icon-newwin" style="display:inline-block;vertical-align:text-bottom"></span>');
	buttons.each(function() {
		var add = $(this);
		var list = add.parent();
		var id = add.attr("id");
		if (!id) {
			var count = list.attr("id") ? list.attr("id") : (++iframe_counter);
			id = "dialog-" + count;
		}
		var frame = id + "-iframe";
		var title = '';
		if (list.attr("id")) {
			title = $("label[for='" + list.attr("id") + "']").text();
		}
		if (!title) {
			title = list.find("label").text();
		}
		var iframe = $("<iframe></iframe>");
		iframe.attr("name", frame);
		iframe.attr("src", 'about:blank');
		iframe.bind('calliCreate', function(event) {
			var de = jQuery.Event('calliLink');
			de.location = event.about;
			$(add).trigger(de);
		});
		list.bind('calliLinked', function() {
			iframe.dialog('close');
		});
		add.click(function(e) {
			iframe.dialog({
			    title: title,
			    autoOpen: false,
			    modal: false,
			    draggable: true,
			    resizable: true,
			    autoResize: true,
				width: 320,
				height: 480
			});
			iframe.bind("dialogclose", function(event, ui) {
				iframe.dialog("destroy");
				add.focus();
			});
			iframe.dialog("open");
			iframe.css('width', '100%');
			if (add.attr("data-search") && add.attr("data-search").indexOf('{searchTerms}') >= 0) {
				var dialogTitle = iframe.parents(".ui-dialog").find(".ui-dialog-title");
				var searchTerms = id + "-input";
				var form = $("<form>"
					+ "<div class='lookup'>"
					+ "<input name='q' title='Lookup..' id='" + searchTerms + "' /></div>"
					+ "</form>");
				form.css('position', "absolute");
				form.css('top', dialogTitle.offset().top - iframe.parent().offset().top - 5);
				form.css('right', 30);
				iframe.before(form);
				form.submit(function(event) {
					var terms = document.getElementById(searchTerms).value;
					var searchUrl = add.attr("data-search").replace('{searchTerms}', encodeURIComponent(terms));
					listSearchResults(searchUrl, iframe[0], add);
					event.preventDefault();
					return false;
				});
			}
			iframe[0].src = add.attr("data-dialog");
		});
	});
}

function listSearchResults(url, iframe, button) {
	jQuery.get(url, function(data) {
		if (data) {
			var result = $(data).children("[about],[resource]");
			result.find(':input').remove();
			var ul = $("<ul/>");
			result.each(function() {
				var about = $(this).attr("about");
				if (!about) {
					about = $(this).attr("resource");
				}
				if (about && about.indexOf('[') < 0) {
					var li = $("<li/>");
					var link = $('<a/>');
					link.attr("class", "view");
					link.attr("href", about);
					link.append($(this).clone());
					li.append(link);
					ul.append(li);
				}
			});
			var html = ul.html();
			if (html) {
				iframe.src = "about:blank";
	            var doc = iframe.contentWindow.document;
				doc.open();
				doc.write("<ul>" + html + "</ul>");
				doc.close();
				$('a', doc).click(function(event) {
					var de = jQuery.Event('calliLink');
					de.location = this.href;
					$(button).trigger(de);
					if (de.isDefaultPrevented()) {
						event.preventDefault();
						return false;
					}
					return true;
				});
			} else {
				iframe.src = "about:blank";
	            var doc = iframe.contentWindow.document;
				doc.open();
				doc.write('<p style="text-align:center">No match found</p>');
				doc.close();
			}
		}
	});
}

})(jQuery);

