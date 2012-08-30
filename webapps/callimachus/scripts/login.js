// login.js

(function($, jQuery){

$(document).ready(function() {
    $("#login-link").click(function(event) {
        var login = jQuery.Event("calliLogin");
        $(this).trigger(login);
        if (login.isDefaultPrevented()) {
            event.preventDefault();
            return false;
        }
        return true;
    });
});

$(document).bind("calliLoggedIn", function(event) {
    $(document).ready(function() {
        if (event.title) {
            $("#profile-link").text(event.title);
        }
    });
});

$(document).ready(function() {
    $("#logout-link").click(function(event) {
        var logout = jQuery.Event("calliLogout");
        $(this).trigger(logout);
        if (logout.isDefaultPrevented()) {
            event.preventDefault();
            return false;
        }
        return true;
    });
});

})(jQuery, jQuery);

