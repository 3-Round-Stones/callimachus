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
		try {
			if (window.frameElement && parent.jQuery) {
				var ce = parent.jQuery.Event("calliCreate");
				ce.location = window.calli.viewpage(redirect);
				ce.about = redirect;
				ce.rdfType = "foaf:Image";
				parent.jQuery(frameElement).trigger(ce);
				if (ce.isDefaultPrevented()) {
					return;
				}
			}
		} catch (e) { }
		var event = jQuery.Event("calliRedirect");
		event.location = window.calli.viewpage(redirect);
		form.trigger(event);
		if (!event.isDefaultPrevented()) {
			location.replace(event.location);
		}
	} else {
		var h1 = $(doc).find('h1').html();
		var pre = $(doc).find('pre').html();
		form.trigger("calliError", h1, pre);
	}
}

})(window.jQuery);

