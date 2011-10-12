// create-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

calli.createResource = function(node) {
	var href = $(node).attr("href");
	var list = $(node).parents('[dropzone]');
	if (!href || !list.length)
		return true;
	var title = '';
	if (list.attr("id")) {
		title = $("label[for='" + list.attr("id") + "']").text();
	}
	if (!title) {
		title = list.find("label").text();
	}
	var options = {
		onmessage: function(event) {
			if (event.data.indexOf('PUT src\n') == 0) {
				var data = event.data;
				var src = data.substring(data.indexOf('\n\n') + 2);
				var uri = calli.listResourceIRIs(src)[0];
				var de = jQuery.Event('calliLink');
				de.location = uri;
				$(node).trigger(de);
			}
		},
		onclose: function() {
			list.unbind('calliLinked', onlinked);
			node.focus();
		}
	};
	if (list.attr("data-search") && list.attr("data-search").indexOf('{searchTerms}') >= 0) {
		options.onlookup = function(terms) {
			var searchUrl = list.attr("data-search").replace('{searchTerms}', encodeURIComponent(terms));
			listSearchResults(searchUrl, dialog, node);
		};
	}
	var dialog = calli.openDialog(href, title, options);
	var onlinked = function() {
		calli.closeDialog(dialog);
	};
	list.bind('calliLinked', onlinked);
	return false;
};

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

