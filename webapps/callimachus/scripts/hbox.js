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
    children.css({"float": "left", "margin-bottom": "0.5ex"});
    
    var wSum, maxW, wMargin;
    children.each(function() {
        if ($(this).is(':first-child')) { // start of an hbox
            $(this).css({"margin-right": "1em"}); // 1st one always gets a margin, simplifies calculation
            wSum = 0;
            maxW = $(this.parentNode).width() - 2; // -2 just to be on the safe side
            wMargin = $(this).outerWidth(true) - $(this).outerWidth(false);
        }
        var 
            wElem = $(this).outerWidth(),
            wAvail = maxW - wSum
        ;
        if (wAvail >= wElem + wMargin) { // enough space for element with full margin
            $(this).css({"clear": "none", "margin-right": "1em"});
            wSum = wSum + wElem + wMargin;
        }
        else if (wAvail >= wElem) { // enough space for element without margin
            $(this).css({"clear": "none", "margin-right": 0});
            wSum = maxW; // force next element on new line
        }
        else { // no space left on this row
            $(this).css({"clear": "left", "margin-right": "1em"});
            wSum = 0;
        }
    });
}

})(window.jQuery);

