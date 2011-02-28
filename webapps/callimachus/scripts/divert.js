// divert.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
	var links = $("a.diverted,a[data-diverted]", event.target);
	if ($(event.target).is("a.diverted,a[data-diverted]")) {
		links = links.add(event.target);
	}
	links.each(function() {
		var href = window.calli.diverted(this.href, this);
		var link = $(this);
		if (this.href != href) {
			if (!link.attr("resource")) {
				link.attr("resource", link.attr("href"));
			}
			this.href = href;
		}
		link.removeAttr("data-diverted");
		if (this.href.indexOf("/diverted;") < 0) {
			link.removeClass("diverted");
		} else {
			link.addClass("diverted");
		}
	});
}

if (!window.calli) {
	window.calli = {};
}

window.calli.diverted = function(url, node) {
    var prefix = location.protocol + '//' + location.host + '/';
	if (url.indexOf(prefix) != 0 && url.indexOf(':') > 0 || url.indexOf('?') > 0 || url.indexOf('#') > 0) {
		if (url.indexOf(':') < 0) {
		    if (node && node.baseURIObject && node.baseURIObject.resolve) {
		        url = node.baseURIObject.resolve(url);
		    } else {
		        var a = document.createElement('a');
		        a.setAttribute('href', url);
		        if (a.href) {
		            url = a.href;
		        }
		    }
		}
    	var path = 'diverted;';
		url = prefix + path + encodeURIComponent(url);
	}
	var query = $(node).attr("data-diverted");
	if (query) {
	    url = url + query;
	}
	return url
}

})(window.jQuery);

