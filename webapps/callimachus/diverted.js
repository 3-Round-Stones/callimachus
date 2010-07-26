// diverted.js

function diverted(url, node) {
	var doc = document
	if (node && node.ownerDocument) {
		doc = node.ownerDocument
	}
	if (doc.defaultView) {
		doc = doc.defaultView
	}
    var prefix = doc.location.protocol + '//' + doc.location.host + '/'
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
	if (target && target.href && target.ownerDocument) {
		if (e.preventDefault) {
    		e.preventDefault()
    	}
		var doc = target.ownerDocument
		if (doc.defaultView) {
			doc = doc.defaultView
		}
		doc.location.href = diverted(target.href, target)
    }
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
