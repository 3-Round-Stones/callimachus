// diverted.js

function diverted(url, node) {
    var prefix = location.protocol + '//' + location.host + '/'
    var scripts = document.getElementsByTagName("script")
    for (var i = 0; i < scripts.length; i++) {
    	var src = scripts[i].src
    	if (src && src.indexOf("/diverted.js") == src.length - 12) {
    		prefix = src.substring(0, src.indexOf('/', src.indexOf("://") + 3) + 1)
    	} 
    }
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
    } else if (url.indexOf(prefix) == 0) {
    	return url
    }
    if (url.lastIndexOf('?') > 0) {
        var uri = url.substring(0, url.lastIndexOf('?'))
        var qs = url.substring(url.lastIndexOf('?'))
        return prefix + path + encodeURIComponent(uri) + qs
    }
    return prefix + path + encodeURIComponent(url) + '?view'
}

function divertedLinkClicked(e) {
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
	win.location.href = diverted(target.href, target)
    return false
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
