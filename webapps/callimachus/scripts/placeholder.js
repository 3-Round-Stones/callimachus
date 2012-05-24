// placeholder.js

(function($){

$(document).ready(function(event) {
    if (!hasPlaceholderSupport()) {
        $(document).bind("DOMNodeInserted", handle);
        handle(event);
    }
});

function hasPlaceholderSupport() {
    var input = document.createElement('input');
    return ('placeholder' in input);
}

function select(node, selector) {
    return $(node).find(selector).andSelf().filter(selector);
}

function handle(event) {
    select(event.target, "input[placeholder]").each(function(i, input) {
        var title = input.getAttribute("placeholder");
        if (title) {
            initInputPromptTitle(input, title);
            input.removeAttribute("placeholder");
        }
    });
}

var counter = 0;

function initInputPromptTitle(input, title) {
    var id = input.id;
    if (!id) {
        id = 'input-' + (++counter);
    }
    var promptSpan = $("<span/>");
    promptSpan.attr("class", "watermark");
    promptSpan.attr("style", "position: absolute; margin: 0px 1ex; cursor: text; vertical-align:top; font-size: inherit");
    promptSpan.attr('id', id + '-prompt');
    promptSpan.attr("title", title);
    promptSpan.text(title);
    promptSpan.bind('mouseover', function() {
        promptSpan.css('display', "none");
    });
    promptSpan.bind('mouseout', function() {
        if(input.value == '' && input!=document.activeElement) {
            promptSpan.css('display', "inline");
        }
    });
    promptSpan.bind('click', function() {
        promptSpan.css('display', "none");
        input.focus();
    });
    if(input.value != '') {
        promptSpan.css('display', "none");
    }
    input.parentNode.insertBefore(promptSpan[0], input);
    input.onfocus = function() {
        promptSpan.css('display', "none");
    };
    input.onblur = function() {
        if(input.value == '') {
            promptSpan.css('display', "inline");
        }
    };
    input.onmouseover = function() {
        promptSpan.css('display', "none");
    };
    input.onmouseout = function() {
        if(input.value == '' && input!=document.activeElement) {
            promptSpan.css('display', "inline");
        }
    };
}

})(window.jQuery);

