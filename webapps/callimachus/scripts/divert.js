// divert.js

(function($){

if (!window.calli) {
	window.calli = {};
}

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
	$("a.diverted,a[data-diverted]", event.target).click(window.calli.divertedLinkClicked);
	if ($(event.target).is("a.diverted,a[data-diverted]")) {
		$(event.target).click(window.calli.divertedLinkClicked);
	}
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
	if ($(node).attr("data-diverted"))
	    return url + $(node).attr("data-diverted");
	return url
}

window.calli.divertedLinkClicked = function(e) {
	var target = e.target;
    if (!target) {
    	target = e.srcElement;
    }
	while (target && !target.href) {
		target = target.parentNode;
	}
	if (!target || !target.href || !target.ownerDocument)
		return true;
	if (e.preventDefault) {
		e.preventDefault();
	}
	var win = target.ownerDocument;
	if (win.defaultView) {
		win = win.defaultView;
	}
	var url = window.calli.diverted(target.href, target);
	if (target.className.match(/\breplace\b/)) {
		setTimeout(function() { win.location.replace(url); }, 0);
	} else {
		setTimeout(function() { win.location.href = url; }, 0);
	}
    return false;
}

})(window.jQuery);

