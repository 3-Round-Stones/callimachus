// iframe-redirect.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
	select(event.target, "iframe.redirect").bind('load', onload);
}

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function onload(event) {
	var doc = this.contentWindow.document;
	if (doc.URL == "about:blank")
		return true;
	var redirect = $(doc).text();
	if (redirect && redirect.indexOf('http') == 0) {
		var event = jQuery.Event("calliRedirect");
		event.location = window.calli.viewpage(redirect);
		$(this).trigger(event);
		if (!event.isDefaultPrevented()) {
			var dataTarget = $(this).attr('data-target');
			if (dataTarget && window.frames[dataTarget]) {
				window.frames[dataTarget].location.href = event.location;
			} else {
				window.location.replace(event.location);
			}
		}
	} else {
		var h1 = $(doc).find('h1').html();
		var pre = $(doc).find('pre').html();
		$(this).trigger("calliError", h1, pre);
	}
}

})(window.jQuery);

