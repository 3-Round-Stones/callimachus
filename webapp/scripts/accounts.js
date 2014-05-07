// accounts.js
/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
(function($,jQuery){

$(document).ready(function(){
    $('a#logout-link').click(function(event) {
        logout(this.href, '/');
        event.preventDefault();
        return false;
    });
});

function logout(url, return_to) {
    jQuery.ajax({ type: 'POST', url: url,
        username: '-', password: 'logout',
        success: function(data) {
            try {
                window.localStorage.removeItem('username');
                window.localStorage.removeItem('userIri');
                window.localStorage.removeItem("digestPassword");
            } catch(e) {}
            $(document).trigger("calliLoggedOut");
            if (return_to) {
                window.location = return_to;
            }
        }
    });
}

if (!window.calli) {
    window.calli = {};
}
window.calli.getUserIri = function() {
    var iri = null;
    try {
        iri = window.localStorage.getItem('userIri');
        if (iri)
            return iri;
    } catch(e) {}
    if (isLoggedIn()) {
        jQuery.ajax({ url: "/?profile", async: false,
            success: function(doc) {
                iri = /resource="([^" >]*)"/i.exec(doc)[1];
                loadProfile(doc);
            }
        });
    }
    return iri;
};

$(document).bind("calliLogin", function(event) {
    try {
        window.localStorage.removeItem('username');
        window.localStorage.removeItem('userIri');
        window.localStorage.removeItem("digestPassword");
    } catch(e) {}
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
            $("#profile-link").contents().filter(function(){
                return this.nodeType === 3 && this.nodeValue.replace(/\s+/g,'');
            }).first().replaceWith(document.createTextNode(event.title));
        }
    });
});

$(document).bind("calliLogout", function(event) {
    logout("/?logout", event.location);
    event.preventDefault();
});

$(document).bind("calliLoggedOut", function(event) {
    document.documentElement.className += ' logout';
    $(document.documentElement).removeClass('login');
});

try {
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
} catch(e) {}

if (isLoggedIn()) {
    // logged in already
    nowLoggedIn();
} else {
    var digestPassword;
    try {
        digestPassword = window.localStorage.getItem("digestPassword");
    } catch(e) {
        digestPassword = null;
    }
    if (digestPassword) {
        // stay signed in with a digest password
        jQuery.ajax({ url: "/?profile",
            username: window.localStorage.getItem("username"),
            password: window.localStorage.getItem("digestPassword"),
            success: function(doc) {
                loadProfile(doc);
                nowLoggedIn();
            },
            error: nowLoggedOut
        });
    } else if (getUsername()) {
        activelyLogin();
    } else {
        nowLoggedOut();
        // hasn't logged in using the login form; is this page protected?
        var xhr = jQuery.ajax({type: 'GET', url: calli.getPageUrl(),
            xhrFields: calli.withCredentials,
            success: function() {
                var cc = xhr.getResponseHeader("Cache-Control");
                if (cc && cc.indexOf("public") < 0) {
                    activelyLogin();
                }
            }
        });
    }
}

function activelyLogin() {
    jQuery.ajax({ url: "/?profile",
        xhrFields: calli.withCredentials,
        success: function(doc) {
            loadProfile(doc);
            nowLoggedIn();
        },
        error: nowLoggedOut
    });
}

function nowLoggedIn() {
    var name = getUsername();
    if (name) {
        try {
            window.localStorage.setItem("username", name);
        } catch(e) {}
        var e = jQuery.Event("calliLoggedIn");
        e.title = name;
        $(document).ready(function() {
            $(document).trigger(e);
        });
    } else {
        nowLoggedOut();
    }
}

function nowLoggedOut() {
    try {
        document.cookie = getUserCookieName() + '=;max-age=0';
    } catch (e) {}
    try {
        window.localStorage.removeItem("username");
        window.localStorage.removeItem("digestPassword");
    } catch(e) {}
    $(document).ready(function() {
        $(document).trigger("calliLoggedOut");
    });
}

function isLoggedIn() {
    var username = getUsername();
    try {
        return username && window.localStorage.getItem("username");
    } catch(e) {
        return username;
    }
}

function getUsername() {
    var pattern = escape(getUserCookieName()).replace(/[\-\.\+\*]/g, "\\$&");
    var hasItem = new RegExp("(?:^|;\\s*)" + pattern + "\\s*\\=\\s*[^;\\s]");
    var regex = new RegExp("(?:^|.*;\\s*)" + pattern + "\\s*\\=\\s*((?:[^;](?!;))+[^;]?).*");
    if (document.cookie && hasItem.test(document.cookie))
        return decodeURIComponent(document.cookie.replace(regex, "$1"));
    return null;
}

function getUserCookieName() {
    var secure = window.location.protocol == 'https:';
    return "username" + window.location.port + (secure ? 's' : '');
}

function loadProfile(doc) {
    var iri = /resource="([^" >]*)"/i.exec(doc);
    if (iri) {
        try {
            // now logged in
            window.localStorage.setItem("userIri", iri[1]);
        } catch(e) {}
    }
}

})(jQuery,jQuery);

