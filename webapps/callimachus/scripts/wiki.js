// wiki.js

(function($){

var creole = new Parse.Simple.Creole();

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
	$("pre.wiki", event.target).each(function(i, node) {
		changeDateLocale(node);
	})
	if ($(event.target).is("pre.wiki")) {
		changeDateLocale(event.target);
	}
}

function initWiki(pre) {
	var text = pre.getAttribute("content") || pre.textContent || pre.innerText;
	var div = document.createElement("div");
	var attrs = wikis[i].attributes;
	for(var j=attrs.length-1; j>=0; j--) {
		div.setAttribute(attrs[j].name, attrs[j].value);
	}
	div.setAttribute("content", text);
	wikis[i].parentNode.replaceChild(div, wikis[i]);
	creole.parse(div, text);
}

})(window.jQuery);

