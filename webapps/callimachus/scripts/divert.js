// divert.js

(function($){

if (window.addEventListener) {
	window.addEventListener("DOMContentLoaded", divertLinks, false)
	window.addEventListener("DOMContentLoaded", replaceLinks, false)
} else {
	window.attachEvent("onload", divertLinks)
	window.attachEvent("onload", replaceLinks)
}

if (!window.calli) {
	window.calli = {}
}

window.calli.diverted = function(url, node) {
    var prefix = location.protocol + '//' + location.host + '/'
    var path = 'diverted;'
    if (url.indexOf(':') < 0) {
        if (node && node.baseURIObject && node.baseURIObject.resolve) {
            url = node.baseURIObject.resolve(url)
        } else {
            var a = document.createElement('a')
            a.setAttribute('href', url)
            if (a.href) {
                url = a.href
            }
        }
    }
	if (url.indexOf(prefix) != 0 || url.indexOf('?') > 0 || url.indexOf('#') > 0) {
		url = prefix + path + encodeURIComponent(url)
	}
	if ($(node).attr("data-diverted"))
	    return url + $(node).attr("data-diverted")
	return url
}

window.calli.divertedLinkClicked = function(e) {
	var target = e.target
    if (!target) {
    	target = e.srcElement
    }
	while (target && !target.href) {
		target = target.parentNode
	}
	if (!target || !target.href || !target.ownerDocument)
		return true;
	if (e.preventDefault) {
		e.preventDefault()
	}
	var win = target.ownerDocument
	if (win.defaultView) {
		win = win.defaultView
	}
	if (target.className.match(/\breplace\b/)) {
		win.location.replace(window.calli.diverted(target.href, target))
	} else {
		win.location.href = window.calli.diverted(target.href, target)
	}
    return false
}

function divertLinks() {
	var links = $("a.diverted,a[data-diverted]").click(window.calli.divertedLinkClicked)
}

function replaceLinkClicked(e) {
	var target = e.target
    if (!target) {
    	target = e.srcElement
    }
	while (target && !target.href) {
		target = target.parentNode
	}
	if (!target || !target.href || !target.ownerDocument)
		return true;
	if (e.preventDefault) {
		e.preventDefault()
	}
	var win = target.ownerDocument
	if (win.defaultView) {
		win = win.defaultView
	}
	win.location.replace(target.href)
    return false
}

function replaceLinks() {
	var links = $("a.replace").filter(function(){return !$(this).hasClass("diverted") && !$(this).attr("data-diverted")}).click(replaceLinkClicked)
}

})(window.jQuery)

