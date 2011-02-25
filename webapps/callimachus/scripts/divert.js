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
		this.href = window.calli.diverted(this.href, this);
		$(this).removeAttr("data-diverted");
		if (this.href.indexOf("/diverted;") < 0) {
			$(this).removeClass("diverted");
		} else {
			$(this).addClass("diverted");
		}
	});
}

if (!window.calli) {
	window.calli = {};
}

window.calli.diverted = function(url, node) {
    var prefix = location.protocol + '//' + location.host + '/';
    var path = 'diverted;';
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
	if (url.indexOf(prefix) != 0 || url.indexOf('?') > 0 || url.indexOf('#') > 0) {
		url = prefix + path + encodeURIComponent(url);
	}
	var query = $(node).attr("data-diverted");
	if (query) {
	    url = url + query;
	}
	return url
}

})(window.jQuery);

