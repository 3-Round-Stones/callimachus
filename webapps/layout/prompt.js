// prompt.js

function hideFluffIfInFrame() {
	try {
		if (window.frameElement) {
			hideSiblings(document.getElementById("content"))
		}
	} catch(e) {}
}

function hideSiblings(node) {
	if (node && node.parentNode) {
		hideSiblings(node.parentNode)
		var siblings = node.parentNode.childNodes
		for (var i = 0; i < siblings.length; i++) {
			if (siblings[i].nodeType == 1 && siblings[i] != node) {
				siblings[i].style.display = "none"
			}
		}
		node.style.border = "none"
		node.style.margin = "0px"
	}
}

if (window.attachEvent) {
	window.attachEvent("onload", hideFluffIfInFrame)
} else {
	window.addEventListener("DOMContentLoaded", hideFluffIfInFrame, false)
}
