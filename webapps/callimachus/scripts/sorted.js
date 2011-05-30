// sorted.js

(function($){

$(document).ready(handle);

$(document).bind("DOMNodeInserted", handle);

function handle(event) {
	$(".sorted", event.target).each(function(i, node) {
		sortElements(node);
	})
	if ($(event.target).is(".sorted")) {
		sortElements(event.target);
	}
}

function sortElements(node) {
	var nodes = node.childNodes;
	var list = [nodes.length];
	for (var i = 0; i < nodes.length; i++) {
		list[i] = nodes[i];
	}
	var exclude = $(node).find(".sorted").find(".asc, .desc");
	list.sort(function(a, b) {
		if (a.nodeType < b.nodeType) return -1;
		if (a.nodeType > b.nodeType) return 1;
		if (a.nodeType != 1) return 0;
		var a1 = $(a).find(".asc").not(exclude).text();
		var a2 = $(b).find(".asc").not(exclude).text();
		try {
			var i1 = parseInt(a1);
			var i2 = parseInt(a2);
			if (i1 > i2) return 1;
			if (i1 < i2) return -1;
		} catch (e) {}
		if (a1 > a2) return 1;
		if (a1 < a2) return -1;
		var d1 = $(a).find(".desc").not(exclude).text();
		var d2 = $(b).find(".desc").not(exclude).text();
		try {
			var i1 = parseInt(d1);
			var i2 = parseInt(d2);
			if (i1 > i2) return -1;
			if (i1 < i2) return 1;
		} catch (e) {}
		if (d1 > d2) return -1;
		if (d1 < d2) return 1;
		return 0;
	});
	for (var i = 0; i < nodes.length; i++) {
		node.appendChild(list[i]);
	}
}

})(window.jQuery);

