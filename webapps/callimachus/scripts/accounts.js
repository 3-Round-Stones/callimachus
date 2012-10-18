// accounts.js

(function($,jQuery){

$(document).ready(function(){
    $('a#login-link,a#profile-link').mousedown(function(event) {
        this.href = divertIfForeign(this.href);
        return true;
    });
    $('a#logout-link').click(function(event) {
        logout(divertIfForeign(this.href));
        event.preventDefault();
        return false;
    });
});

function divertIfForeign(url) {
    var same = location.protocol + '//' + location.host + '/';
    if (url.indexOf(same) != 0 && url.indexOf('?') >= 0) {
        return calli.diverted(url.substring(0, url.indexOf('?')), url.substring(url.indexOf('?') + 1));
    } else if (url.indexOf(same) != 0) {
        return calli.diverted(url);
    }
    return url;
}

function logout(url) {
    jQuery.ajax({ type: 'POST', url: url,
        username: 'logout', password: 'please',
        success: function(data) {
            if (window.localStorage) {
                localStorage.removeItem('username');
                localStorage.removeItem('userIri');
                localStorage.removeItem("digestPassword");
            }
            $(document).trigger("calliLoggedOut");
            window.location = "/";
        }
    });
}

if (!window.calli) {
    window.calli = {};
}
window.calli.getUserIri = function() {
    var iri = null;
    if (window.localStorage) {
        iri = localStorage.getItem('userIri');
        if (iri)
            return iri;
    }
    if (isLoggedIn()) {
        jQuery.ajax({ url: "/?profile", async: false,
            beforeSend: calli.withCredentials,
            success: function(doc) {
                iri = /resource="([^" >]*)"/i.exec(doc)[1];
                loadProfile(doc);
            }
        });
    }
    return iri;
};

$(document).bind("calliLogin", function(event) {
    if (window.localStorage) {
        localStorage.removeItem('username');
        localStorage.removeItem('userIri');
        localStorage.removeItem("digestPassword");
    }
    var return_to = window.location.href;
    window.location = "/?login&return_to=" + encodeURIComponent(return_to);
    event.preventDefault();
});

$(document).bind("calliLoggedIn", function(event) {
    document.documentElement.className += ' login';
    $(document.documentElement).removeClass('logout');
});

$(document).bind("calliLoggedIn", function(event) {
    $(document).ready(function() {
        if (event.title) {
            $("#profile-link").text(event.title);
        }
    });
});

$(document).bind("calliLogout", function(event) {
    logout("/?logout");
    event.preventDefault();
});

$(document).bind("calliLoggedOut", function(event) {
    document.documentElement.className += ' logout';
    $(document.documentElement).removeClass('login');
});

if (window.localStorage) {
    var broadcastLoggedIn = false;
    $(document).bind("calliLoggedIn", function(event) {
        broadcastLoggedIn = true;
    });
    $(document).bind("calliLoggedOut", function(event) {
        broadcastLoggedIn = false;
    });
    var storageChanged = function() {
        if (isLoggedIn() && !broadcastLoggedIn) {
            nowLoggedIn();
            broadcastLoggedIn = true;
        } else if (!isLoggedIn() && broadcastLoggedIn){
             nowLoggedOut();
             broadcastLoggedIn = false;
        }
        return true;
    };
    $(window).bind('storage', storageChanged);
    $(document).bind('storage', storageChanged); // IE
}

if (isLoggedIn()) {
    // logged in already
    nowLoggedIn();
} else if (window.localStorage && localStorage.getItem("digestPassword")) {
    // stay signed in with a digest password
    jQuery.ajax({ url: "/?profile",
        username: localStorage.getItem("username"),
        password: localStorage.getItem("digestPassword"),
        success: function(doc) {
            loadProfile(doc);
            nowLoggedIn();
        },
        error: nowLoggedOut
    });
} else {
    // not logged in yet
    nowLoggedOut();
}

function nowLoggedIn() {
    if (isLoggedIn()) {
        var name = getUsername();
        if (window.localStorage) {
            localStorage.setItem("username", name);
        }
        var e = jQuery.Event("calliLoggedIn");
        e.title = name;
        $(document).ready(function() {
            $(document).trigger(e);
        });
    }
}

function nowLoggedOut() {
    if (!isLoggedIn()) {
        if (window.localStorage) {
            localStorage.removeItem("username");
        }
        $(document).ready(function() {
            $(document).trigger("calliLoggedOut");
        });
    }
}

function isLoggedIn() {
    return document.cookie && /(?:^|;\s*)username\s*\=/.test(document.cookie);
}

function getUsername() {
    if (isLoggedIn())
        return decodeURIComponent(document.cookie.replace(/(?:^|.*;\s*)username\s*\=\s*((?:[^;](?!;))*[^;]?).*/, "$1"));
    return null;
}

function loadProfile(doc) {
    var iri = /resource="([^" >]*)"/i.exec(doc);
    if (iri) {
        if (window.localStorage) {
            // now logged in
            localStorage.setItem("userIri", iri[1]);
        }
    }
}

})(jQuery,jQuery);

