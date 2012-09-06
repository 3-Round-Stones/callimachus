// disabled-button.js

(function($){

$(document).ready(function() {
    $("button", document).click(flashButton);
});
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
    $("button", event.target).click(flashButton);
    if ($(event.target).is("button")) {
        $(event.target).click(flashButton);
    }
}

function flashButton(event) {
    var button = $(this);
    setTimeout(function() {
        if (!button.attr('disabled')) {
            button.attr('disabled', 'disabled');
            button.addClass("disabbled");
            setTimeout(function() {
                button.removeAttr('disabled');
                button.removeClass("disabbled");
            }, 5000);
            button.focus(function() {
                button.removeAttr('disabled');
                button.removeClass("disabbled");
                return true;
            });
        }
    }, 0); // yield
    return true;
}

})(window.jQuery);
