// accounts.js

(function($,jQuery){

if (!window.calli) {
    window.calli = {};
}
window.calli.getUserIri = function() {
    var iri = null;
    if (window.sessionStorage) {
        iri = sessionStorage.getItem('UserIri');
        if (iri)
            return iri;
    }
    jQuery.ajax({ url: "/?login", async: false,
        beforeSend: withCredentials,
        success: function(doc) {
            iri = /resource="([^" >]*)"/i.exec(doc)[1];
        }
    });
    return iri;
};

$(document).bind("calliLogin", function(event) {
    // remove bogus data
    if (window.sessionStorage) {
        sessionStorage.removeItem('Name');
        sessionStorage.removeItem('UserIri');
        localStorage.removeItem('Authorization');
    }
    var options = {type: "GET", url: "/?login",
        success: function(doc) {
            var iri = /resource="([^" >]*)"/i.exec(doc);
            if (iri) {
                try {
                    if (window.sessionStorage) {
                        // now logged in
                        sessionStorage.setItem("UserIri", iri[1]);
                    }
                } catch (e) { }
            }
            var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(doc);
            if (title) {
                try {
                    if (window.sessionStorage) {
                        // now logged in
                        sessionStorage.setItem("Name", title[1]);
                        localStorage.setItem("Name", title[1]);
                    }
                } catch (e) { }
                document.documentElement.className += ' auth';
                var e = jQuery.Event("calliLoggedIn");
                e.title = title[1];
                $(document).ready(function() {
                    $(document.documentElement).removeClass('noauth');
                    $(document).trigger(e);
                });
            }
            if (window.localStorage && event.username) {
                var auth = "User " + event.username;
                if (event.remember) {
                    auth = event.username + ":" + event.password;
                    if (window.btoa) {
                        auth = "Basic " + window.btoa(auth);
                    } else {
                        auth = "Credentials " + auth;
                    }
                }
                try {
                    localStorage.setItem('Authorization', auth);
                } catch (e) { }
            }
            if (!event.isDefaultPrevented() && event.location) {
                location = event.location;
            } else if (!event.isDefaultPrevented() && event.location) {
                location.reload(false);
            }
        }
    };
    if (event.username) {
        options.username = event.username;
    }
    if (event.password) {
        options.password = event.password;
    } else {
        options.beforeSend = withCredentials;
    }
    try {
        jQuery.ajax(options);
    } catch (e) {
        // Opera don't support spaces in ajax passwords
        if (options.password) {
            delete options.password;
            jQuery.ajax(options);
        }
    }
});

$(document).bind("calliLogout", function(event) {
    if (!window.sessionStorage || sessionStorage.getItem('Name')) {
        jQuery.ajax({ type: 'GET', url: "/?logout",
            username: 'logout', password: 'please',
            success: function(data) {
                location = "/";
            }
        });
    }
    if (window.sessionStorage) {
        sessionStorage.removeItem('Name');
        localStorage.removeItem('Authorization');
        localStorage.removeItem('NotLoginCount');
        localStorage.removeItem('LoginCount');
        localStorage.removeItem('Name');
    }
});

if (window.localStorage) {
    // IE8 doesn't support event.key
    var oldName = localStorage.getItem('Name');
    var oldNotLoginCount = localStorage.getItem('NotLoginCount');
    var oldLoginCount = localStorage.getItem('LoginCount');

    var currently = '';

    $(document).bind("calliLoggedIn", function(event) {
        currently = event.title;
    });

    $(document).bind("calliLoggedOut", function() {
        currently = '';
    });

    var handleStorageEvent = function(e) {
        var newName = localStorage.getItem('Name');
        var newNotLoginCount = localStorage.getItem('NotLoginCount');
        var newLoginCount = localStorage.getItem('LoginCount');
        if (newName != oldName) {
            oldName = newName;
            if (currently && !newName) {
                // now logged out
                document.documentElement.className += ' noauth';
                sessionStorage.removeItem('Name');
                $(document).ready(function() {
                    $(document.documentElement).removeClass('auth');
                    $(document).trigger("calliLoggedOut");
                });
                location = "/";
            } else if (!currently && newName) {
                // now logged in
                sessionStorage.setItem("Name", newName);
                document.documentElement.className += ' auth';
                var e = jQuery.Event("calliLoggedIn");
                e.title = newName;
                $(document).ready(function() {
                    $(document.documentElement).removeClass('noauth');
                    $(document).trigger(e);
                });
            }
        }
        if (newNotLoginCount != oldNotLoginCount) {
            oldNotLoginCount = newNotLoginCount;
            if (currently && newNotLoginCount) {
                // a window is not sure if we are logged in
                localStorage.setItem("Name", currently);
                var count = localStorage.getItem('LoginCount');
                localStorage.setItem('LoginCount', count ? parseInt(count) + 1 : 1);
            } 
        }
        if (newLoginCount != oldLoginCount) {
            oldLoginCount = newLoginCount;
            if (!currently && newLoginCount) {
                // another window says we are logged in
                var value = localStorage.getItem("Name");
                sessionStorage.setItem("Name", value);
                document.documentElement.className += ' auth';
                var e = jQuery.Event("calliLoggedIn");
                e.title = value;
                $(document).ready(function() {
                    $(document.documentElement).removeClass('noauth');
                    $(document).trigger(e);
                });
            } 
        }
        return true;
    };
    $(window).bind('storage', handleStorageEvent);
    $(document).bind('storage', handleStorageEvent); // IE
}

if (window.sessionStorage && sessionStorage.getItem("Name")) {
    // logged in already
    document.documentElement.className += ' auth';
    var name = sessionStorage.getItem("Name");
    var e = jQuery.Event("calliLoggedIn");
    e.title = name;
    $(document).ready(function() {
        $(document).trigger(e);
    });
} else {
    document.documentElement.className += ' noauth';
    $(document).ready(function() {
        $(document).trigger("calliLoggedOut");
    });
    if (window.localStorage && localStorage.getItem('Authorization')) {
        // was logged in previously
        var auth = localStorage.getItem('Authorization');
        if (auth && auth.indexOf("Basic ") == 0) {
            // remember me Firefox, Chrome, Safari
            var up = window.atob(auth.substring("Basic ".length));
            var event = jQuery.Event("calliLogin");
            event.preventDefault(); // don't reload page
            event.username = up.substring(0, up.indexOf(':'));
            event.password = up.substring(up.indexOf(':') + 1);
            event.remember = true;
            $(document).trigger(event);
            return;
        } else if (auth && auth.indexOf("Credentials ") == 0) {
            // remember me IE
            var up = auth.substring("Credentials ".length);
            var event = jQuery.Event("calliLogin");
            event.preventDefault(); // don't reload page
            event.username = up.substring(0, up.indexOf(':'));
            event.password = up.substring(up.indexOf(':') + 1);
            event.remember = true;
            $(document).trigger(event);
            return;
        } else if (localStorage.getItem('Name')) {
            // was logged in before, prompt to log back in
            try {
                var count = localStorage.getItem('NotLoginCount');
                localStorage.setItem('NotLoginCount', count ? parseInt(count) + 1 : 1);
            } catch (e) { }
            setTimeout(function() {
                $(document).ready(function() {
                    $(document).trigger("calliLogInPrompt");
                });
            }, 0); // check if already logged in
        }
    }
    // hasn't logged in using the login form; is this page protected?
    var xhr = jQuery.ajax({type: 'GET', url: calli.getPageUrl(),
        beforeSend: withCredentials,
        success: function() {
            if (xhr.getResponseHeader("Authentication-Info")) { 
                var event = jQuery.Event("calliLogin");
                event.preventDefault(); // don't reload page
                $(document).trigger(event);
            } else if (!xhr.getAllResponseHeaders()) { // Opera sends empty response; try again w/o cache
                xhr = jQuery.ajax({type: 'GET', url: calli.getPageUrl(),
                    beforeSend: function(xhr) {
                        xhr.setRequestHeader('Cache-Control', 'no-cache');
                        withCredentials(xhr);
                    },
                    success: function() {
                        if (xhr.getResponseHeader("Authentication-Info")) {
                            var event = jQuery.Event("calliLogin");
                            event.preventDefault(); // don't reload page
                            $(document).trigger(event);
                        }
                    }
                });
            }
        }
    });
}

function withCredentials(req) {
    try {
        req.withCredentials = true;
    } catch (e) {}
}

})(jQuery,jQuery);

