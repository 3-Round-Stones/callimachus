// diverted.js

function diverted(url, node) {
    var prefix = document.location.protocol + '//' + document.location.host + '/diverted;';
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
    } else if (url.indexOf(prefix) == 0) {
    	return url;
    }
    if (url.lastIndexOf('?') > 0) {
        var uri = url.substring(0, url.lastIndexOf('?'));
        var qs = url.substring(url.lastIndexOf('?'));
        return prefix + encodeURIComponent(uri) + qs;
    }
    return prefix + encodeURIComponent(url) + '?view';
}

function divertedLinkClicked(e) {
    e.preventDefault();
    document.location.href = diverted(this.href, this);
    return false;
}

function divertLinks() {
	var links = document.getElementsByTagName("a")
	for (var i=0; i<links.length; i++) {
		var link = links.item(i)
		var css = link.className
		if (css && css.match(/\bdiverted\b/)) {
			if (link.attachEvent) {
				link.attachEvent("onclick", divertedLinkClicked)
			} else {
				link.addEventListener("click", divertedLinkClicked, false)
			}
		}
	}
}

if (window.attachEvent) {
	window.attachEvent("onload", divertLinks)
} else {
	window.addEventListener("DOMContentLoaded", divertLinks, false)
}
