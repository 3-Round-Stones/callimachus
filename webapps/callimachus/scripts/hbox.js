// hbox.js

(function($){

$(document).ready(function(){hbox($('.hbox'))});
$(document).bind("DOMNodeInserted load", handle);
$(window).resize(function(){hbox($('.hbox'))});

function select(node, selector) {
    var set = $(node).find(selector).andSelf();
    set = set.add($(node).parents(selector));
    return set.filter(selector);
}

function handle(event) {
    hbox(select(event.target, ".hbox"));
}

function hbox(element) {
    element.css("overflow", "hidden");
    var children = element.children();
    children.css("float", "left").css("clear", "none").css("margin-bottom", "0.5ex");
    var siblings = children.filter(':not(:first-child)');
    siblings.css("margin-left", "1em");
    setTimeout(function() { // reposition children first
        siblings.css("clear", function() {
            var prev = prevElement(this);
            if (prev.length && $(this).position().top > prev.position().top) {
                $(this).css("margin-left", "0px");
                return "left";
            } else {
                $(this).css("margin-left", "1em");
                return "none";
            }
        });
    }, 0);
}

function prevElement(element) {
    var prev = $(element).prev();
    while (prev.length && !prev.is(':visible')) {
        prev = prev.prev();
    }
    return prev;
}

})(window.jQuery);

