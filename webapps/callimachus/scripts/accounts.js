// accounts.js

(function($,jQuery){

if (!window.calli) {
    window.calli = {};
}
window.calli.getUserIri = function() {
    var iri = null;
    if (window.localStorage) {
        iri = localStorage.getItem('UserIri');
        if (iri)
            return iri;
    }
    jQuery.ajax({ url: "/?profile", async: false,
        beforeSend: withCredentials,
        success: function(doc) {
            iri = /resource="([^" >]*)"/i.exec(doc)[1];
        }
    });
    return iri;
};

var userName = null;

$(document).bind("calliLogin", function(event) {
    if (window.localStorage) {
        localStorage.removeItem('UserName');
        localStorage.removeItem('UserIri');
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
        if (!event.title || event.title != localStorage.getItem("UserName")) {
            nowLoggedIn(true);
        }
    }
});

$(document).bind("calliLogout", function(event) {
    jQuery.ajax({ type: 'GET', url: "/?logout",
        username: 'logout', password: 'please',
        success: function(data) {
            if (window.localStorage) {
                localStorage.removeItem('UserName');
                localStorage.removeItem('UserIri');
            }
            $(document).trigger("calliLoggedOut");
            window.location = "/";
        }
    });
    event.preventDefault();
});

$(document).bind("calliLoggedOut", function(event) {
    document.documentElement.className += ' logout';
    $(document.documentElement).removeClass('login');
    userName = null;
});

if (window.localStorage) {
    var storageChanged = function() {
        var newName = localStorage.getItem('UserName');
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

if (window.localStorage && localStorage.getItem("UserName")) {
    // logged in already or hasn't logged out yet
    var name = localStorage.getItem("UserName");
    var e = jQuery.Event("calliLoggedIn");
    e.title = name;
    $(document).ready(function() {
        $(document).trigger(e);
    });
} else {
    $(document).ready(function() {
        $(document).trigger("calliLoggedOut");
    });
    // hasn't logged in using the login form; is this page protected?
    var xhr = jQuery.ajax({type: 'GET', url: calli.getPageUrl(),
        beforeSend: withCredentials,
        success: function() {
            if (xhr.getResponseHeader("Authentication-Info")) {
                nowLoggedIn();
            } else if (!xhr.getAllResponseHeaders()) { // Opera sends empty response; try again w/o cache
                xhr = jQuery.ajax({type: 'GET', url: calli.getPageUrl(),
                    beforeSend: function(xhr) {
                        xhr.setRequestHeader('Cache-Control', 'no-cache');
                        withCredentials(xhr);
                    },
                    success: function() {
                        if (xhr.getResponseHeader("Authentication-Info")) {
                            nowLoggedIn();
                        }
                    }
                });
            }
        }
    });
}

function nowLoggedIn(sync) {
    jQuery.ajax({ url: "/?profile",
        beforeSend: withCredentials,
        async: !sync,
        success: function(doc) {
            var iri = /resource="([^" >]*)"/i.exec(doc);
            var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(doc);
            if (iri && title) {
                if (window.localStorage) {
                    // now logged in
                    localStorage.setItem("UserName", title[1]);
                    localStorage.setItem("UserIri", iri[1]);
                }
                var e = jQuery.Event("calliLoggedIn");
                e.title = title[1];
                $(document).ready(function() {
                    $(document).trigger(e);
                });
            }
        }
    });
};

function withCredentials(req) {
    try {
        req.withCredentials = true;
    } catch (e) {}
}

})(jQuery,jQuery);

