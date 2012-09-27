// login.js

(function($, jQuery){

$(document).ready(function() {
    $("#logout-link").click(function(event) {
        $(document).trigger(jQuery.Event("calliLogout"));
        if (event.preventDefault) {
            event.preventDefault();
        }
        return false;
    });
});

var loginForm;

$(document).bind("calliLoggedIn", function(event) {
    $(document).ready(function() {
        var title = event.title;
        $("#profile-link").text(title);
        if (loginForm) {
            $(loginForm).remove();
            $("#login-form").remove();
            loginForm = null;
        }
    });
});

$(document).bind("calliLoggedOut", function(event) {
    $(document).ready(function() {
        if (!loginForm) {
            loginForm = createLoginButton();
        }
    });
});

function createLoginButton() {
    if ($('#login').length) {
        var loginForm = $('<form />')[0];
        $(loginForm).attr('action', $('#profile-link')[0].getAttribute('href'));
        $(loginForm).css('display', "inline-block");
        var button = $('<button type="submit" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-primary" style="margin:0px" />');
        var icon = $('<span class="ui-button-icon-primary ui-icon ui-icon-circle-arrow-s" style="position:absolute;top:50%;margin-top:-8px;height:16px;width:16px;"> </span>');
        var text = $('<span class="ui-button-text" style="padding-left: 22px;">Login</span>');
        button.append(icon);
        button.append(text);
        $(loginForm).append(button);
        $('#login').append(loginForm);
        $(loginForm).submit(login);
        return loginForm;
    }
    return null;
}

function login(event) {
    try {
        event.preventDefault();
        $(document).trigger("calliLogin");
    } catch (e) {
        throw calli.error(e);
    }
    return false;
}

})(jQuery, jQuery);

