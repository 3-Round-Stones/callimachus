// wiki.js

(function($){

var creole = new Parse.Simple.Creole();

$(document).ready(function() {
    $("pre.wiki", document).each(function() {
        initWiki(this);
    })
});
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
    $("pre.wiki", event.target).each(function(i, node) {
        initWiki(node);
    })
    if ($(event.target).is("pre.wiki")) {
        initWiki(event.target);
    }
}

function initWiki(pre) {
    var text = pre.textContent || pre.innerText;
    var div = document.createElement("div");
    var attrs = pre.attributes;
    for(var j=attrs.length-1; j>=0; j--) {
        div.setAttribute(attrs[j].name, attrs[j].value);
    }
    if (!div.getAttribute("content")) {
        div.setAttribute("content", text);
    }
    pre.parentNode.replaceChild(div, pre);
    creole.parse(div, text);
}

})(window.jQuery);

