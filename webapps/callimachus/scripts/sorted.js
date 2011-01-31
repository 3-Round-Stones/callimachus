// sorted.js

(function($){

if (window.addEventListener) {
	window.addEventListener("DOMContentLoaded", sortElements, false)
} else {
	window.attachEvent("onload", sortElements)
}

function sortElements() {
	var elements = $(".sorted")
	for (var e = 0; e < elements.length; e++) {
		var node = elements[e]
		var nodes = node.childNodes
		var list = [nodes.length]
		for (var i = 0; i < nodes.length; i++) {
			list[i] = nodes[i]
		}
		list.sort(function(a, b) {
			if (a.nodeType < b.nodeType) return -1
			if (a.nodeType > b.nodeType) return 1
			if (a.nodeType != 1) return 0
			var a1 = $(a).find(".asc")
			var a2 = $(b).find(".asc")
			if (a1.length && a2.length) {
				var s1 = a1[0].innerHTML.replace(/\s*<[^>]*>\s*/g, " ")
				var s2 = a2[0].innerHTML.replace(/\s*<[^>]*>\s*/g, " ")
				if (s1 < s2) return -1
				if (s1 > s2) return 1
			} else {
				if (a1.length > a2.length) return -1
				if (a1.length < a2.length) return 1
			}
			var d1 = $(a).find(".desc")
			var d2 = $(b).find(".desc")
			if (d1.length && d2.length) {
				var s1 = d1[0].innerHTML.replace(/\s*<[^>]*>\s*/g, " ")
				var s2 = d2[0].innerHTML.replace(/\s*<[^>]*>\s*/g, " ")
				if (s1 > s2) return -1
				if (s1 < s2) return 1
			} else {
				if (d1.length > d2.length) return -1
				if (d1.length < d2.length) return 1
			}
			return 0
		})
		for (var i = 0; i < nodes.length; i++) {
			node.appendChild(list[i])
		}
	}
}

})(window.jQuery)

