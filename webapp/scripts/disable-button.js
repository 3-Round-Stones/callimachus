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
    if (!button.is('.dropdown-toggle')) {
        // yield
        setTimeout(function() {
            if (!button.attr('disabled')) {
                button.attr('disabled', 'disabled');
                button.addClass("disabled");
                setTimeout(function() {
                    button.removeAttr('disabled');
                    button.removeClass("disabled");
                }, 5000);
                button.focus(function() {
                    button.removeAttr('disabled');
                    button.removeClass("disabled");
                    return true;
                });
            }
        }, 0);
    }
    return true;
}

})(window.jQuery);
