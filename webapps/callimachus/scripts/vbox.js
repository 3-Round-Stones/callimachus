// vbox.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);
$(window).resize(function(){vbox($('.vbox'))});

function select(node, selector) {
    var set = $(node).find(selector).andSelf();
    set = set.add($(node).parents(selector));
    return set.filter(selector);
}

function handle(event) {
    vbox(select(event.target, ".vbox"));
}

function vbox(element) {
    var children = element.children().filter(function() {
        var pos = $(this).css('position');
        return pos != 'absolute' && pos != 'fixed' && pos != 'relative';
    });
    children.css("margin-bottom", "0.15em");
    children.filter(':not(:input)').css("display", "table");
    children.filter(':input').css("display", "block");
}

})(window.jQuery);

