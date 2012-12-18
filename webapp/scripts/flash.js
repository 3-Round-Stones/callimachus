// flash.js

(function($){

$(document).error(function(event) {
    if (event.message && !event.isDefaultPrevented()) {
        if (flash(event.message, event.stack)) {
            event.stopPropagation();
        }
    }
});

function flash(message, stack) {
    var msg = $("#calli-error");
    var template = $('#calli-error-template').children();
    if (!msg.length || !template.length)
        return false;
    var widget = template.clone();
    widget.append(message);
    if (stack) {
        var pre = $("<pre/>");
        pre.append(stack);
        pre.hide();
        var more = $('<a/>');
        more.text("»");
        more.click(function() {
            pre.toggle();
        });
        widget.append(more);
        widget.append(pre);
    }
    msg.append(widget);
    scroll(0,0);
    return true;
}

})(window.jQuery);

