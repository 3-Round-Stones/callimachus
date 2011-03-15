// iframe.js

(function($){

if (window.addEventListener) {
	window.addEventListener("DOMContentLoaded", hideFluffIfInFrame, false);
} else {
	window.attachEvent("onload", hideFluffIfInFrame);
}

function hideFluffIfInFrame() {
	try {
		if (window.frameElement) {
			hideSiblings(document.getElementById("content"));
			if ($("#content form").length) { // hide form instructions
				var asides = document.querySelectorAll(".aside");
				for (var i = 0; i < asides.length; i++) {
					asides[i].parentNode.removeChild(asides[i]);
				}
			}
		}
	} catch(e) {}
}

function hideSiblings(node) {
	if (node && node.parentNode) {
		hideSiblings(node.parentNode);
		var siblings = node.parentNode.childNodes;
		var i = siblings.length;
		while (i--) {
			if (siblings[i].nodeType == 1 && siblings[i] != node) {
				siblings[i].style.display = "none";
			} else if (siblings[i].nodeType == 3 || siblings[i].nodeType == 4) {
				node.parentNode.removeChild(siblings[i]);
			}
		}
		node.style.border = "none";
		node.style.margin = "0px";
		node.style.background = "inherit";
	}
}

})(window.jQuery);

