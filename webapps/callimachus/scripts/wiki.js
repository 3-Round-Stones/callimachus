// wiki.js

(function($){

if (window.addEventListener) {
	window.addEventListener("DOMContentLoaded", initWiki, false)
} else {
	window.attachEvent("onload", initWiki)
}

function initWiki() {
	if (window.Parse) {
		var creole = new Parse.Simple.Creole();
		var wikis = $("pre.wiki")
		for (var i = 0; i < wikis.length; i++) {
			var text = wikis[i].getAttribute("content") || wikis[i].textContent || wikis[i].innerText
			var div = document.createElement("div")
			var attrs = wikis[i].attributes
			for(var j=attrs.length-1; j>=0; j--) {
				div.setAttribute(attrs[j].name, attrs[j].value)
			}
			div.setAttribute("content", text)
			wikis[i].parentNode.replaceChild(div, wikis[i])
			creole.parse(div, text)
		}
	}
}

})(window.jQuery)

