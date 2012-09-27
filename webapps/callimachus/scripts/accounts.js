// accounts.js

(function($,jQuery){

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
    jQuery.ajax({ url: "/?profile", async: false,
        beforeSend: withCredentials,
        success: function(doc) {
            iri = /resource="([^" >]*)"/i.exec(doc)[1];
            loadProfile(doc);
        }
    });
    return iri;
};

var userName = null;

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
    userName = event.title;
    if (window.localStorage) {
        if (!event.title || event.title != localStorage.getItem("username")) {
            nowLoggedIn(true);
        }
    }
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

$(document).ready(function(){
    $('a[rel="logout"]').click(function(event) {
        logout(this.href);
        event.preventDefault();
        return false;
    });
});

$(document).bind("DOMNodeInserted",function handle(event) {
    $('a[rel="logout"]', event.target).click(function(event) {
        logout(this.href);
        event.preventDefault();
        return false;
    });
    if ($(event.target).is('a[rel="logout"]')) {
        $(event.target).click(function(event) {
            logout(this.href);
            event.preventDefault();
            return false;
        });
    }
});

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

$(document).bind("calliLoggedOut", function(event) {
    document.documentElement.className += ' logout';
    $(document.documentElement).removeClass('login');
    userName = null;
});

if (window.localStorage) {
    var storageChanged = function() {
        var newName = localStorage.getItem('username');
        if (newName != userName) {
            userName = newName;
            if (!newName) {
                // now logged out
                $(document).ready(function() {
                    $(document).trigger("calliLoggedOut");
                });
            } else if (newName) {
                // now logged in
                var e = jQuery.Event("calliLoggedIn");
                e.title = newName;
                $(document).ready(function() {
                    $(document).trigger(e);
                });
            }
        }
        return true;
    };
    $(window).bind('storage', storageChanged);
    $(document).bind('storage', storageChanged); // IE
}

if (document.cookie && /(?:^|;\s*)username\s*\=/.test(document.cookie)) {
    // logged in already
    var name = decodeURIComponent(document.cookie.replace(/(?:^|.*;\s*)username\s*\=\s*((?:[^;](?!;))*[^;]?).*/, "$1"));
    var e = jQuery.Event("calliLoggedIn");
    e.title = name;
    $(document).ready(function() {
        $(document).trigger(e);
    });
} else if (window.localStorage && localStorage.getItem("digestPassword")) {
    // stay signed in with a digest password
    jQuery.ajax({ url: "/?profile",
        username: localStorage.getItem("username"),
        password: localStorage.getItem("digestPassword"),
        success: loadProfile
    });
} else {
    // not logged in yet
    $(document).ready(function() {
        $(document).trigger("calliLoggedOut");
    });
}

function nowLoggedIn(sync) {
    jQuery.ajax({ url: "/?profile",
        beforeSend: withCredentials,
        async: !sync,
        success: loadProfile
    });
};

function loadProfile(doc) {
    var iri = /resource="([^" >]*)"/i.exec(doc);
    var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(doc);
    if (iri && title) {
        if (window.localStorage) {
            // now logged in
            localStorage.setItem("username", title[1]);
            localStorage.setItem("userIri", iri[1]);
        }
        var e = jQuery.Event("calliLoggedIn");
        e.title = title[1];
        $(document).ready(function() {
            $(document).trigger(e);
        });
    }
}

function withCredentials(req) {
    try {
        req.withCredentials = true;
    } catch (e) {}
}

})(jQuery,jQuery);

