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
	buttons.addClass('ui-state-default');
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
		add.click(function(e) {
			var options = {
				onmessage: function(event) {
					if (event.data.indexOf('PUT src\n') == 0) {
						var data = event.data;
						var src = data.substring(data.indexOf('\n\n') + 2);
						var uri = calli.listResourceIRIs(src)[0];
						var de = jQuery.Event('calliLink');
						de.location = uri;
						$(add).trigger(de);
					}
				},
				onclose: function() {
					list.unbind('calliLinked', onlinked);
					add.focus();
				}
			};
			if (add.attr("data-search") && add.attr("data-search").indexOf('{searchTerms}') >= 0) {
				options.onlookup = function(terms) {
					var searchUrl = add.attr("data-search").replace('{searchTerms}', encodeURIComponent(terms));
					listSearchResults(searchUrl, dialog, add);
				};
			}
			var dialog = calli.openDialog(add.attr("data-dialog"), title, options);
			var onlinked = function() {
				calli.closeDialog(dialog);
			};
			list.bind('calliLinked', onlinked);
		});
	});
}

function listSearchResults(url, win, button) {
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
					link.attr("class", "option");
					link.attr("href", about);
					link.append($(this).html());
					li.append(link);
					ul.append(li);
				}
			});
			var html = ul.html();
			if (html) {
	            var doc = win.document;
				doc.open();
				doc.write("<ul>" + html + "</ul>");
				doc.close();
				$('a.option', doc).click(function(event) {
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
	            var doc = win.document;
				doc.open();
				doc.write('<p style="text-align:center">No match found</p>');
				doc.close();
			}
		}
	});
}

})(jQuery);

