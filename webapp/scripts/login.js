// login.js
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
(function($){

var calli = window.calli || (window.calli={});

calli.login = function(username) {
    if (isLoggedIn() || typeof username == 'string') {
        nowLoggedIn();
    } else {
        nowLoggedOut();
    }
};

$(window).bind('storage', calli.login);
$(document).bind('storage', calli.login); // IE

calli.logout = function(realm) {
    var url = (realm || '/') + "?logout";
    return calli.resolve($.ajax({ type: 'POST', url: url,
        username: '-', password: 'logout'
    })).then(nowLoggedOut);
};

calli.getCurrentUserAccount = function() {
    return calli.resolve().then(function(){
        try {
            return window.localStorage.getItem('userIri');
        } catch(e) {}
    }).then(function(iri){
        if (iri || !isLoggedIn()) return iri;
        return calli.getText("/?profile").then(readUserResource);
    });
    return iri;
};

calli.getCurrentUserName = function(){
    return promiseUserName;
};

if (isLoggedIn())
    $(document.documentElement).addClass('login');
else
    $(document.documentElement).addClass('logout');

var promiseUserName = calli.resolve().then(function(){
    if (isLoggedIn()) return true;
    var digestProfile;
    var digestPassword;
    try {
        digestPassword = window.localStorage.getItem("digestPassword");
        digestProfile = window.localStorage.getItem("digestProfile");
    } catch(e) {
        digestPassword = null;
        digestProfile = null;
    }
    if (digestPassword || digestProfile) {
        // stay signed in with a digest password
        return calli.resolve($.ajax({ url: digestProfile || "/?profile",
            username: window.localStorage.getItem("username"),
            password: digestPassword
        })).then(readUserResource);
    } else if (getUsername()) {
        return activelyLogin();
    } else {
        // hasn't logged in using the login form; is this page protected?
        var xhr = $.ajax({type: 'HEAD', url: calli.getPageUrl(),
            xhrFields: {withCredentials: true}
        });
        return calli.resolve(xhr).then(function() {
            var cc = xhr.getResponseHeader("Cache-Control");
            if (cc && cc.indexOf("public") < 0) {
                return activelyLogin();
            } else {
                return null;
            }
        });
    }
}).then(function(loggedIn){
    if (loggedIn) {
        return nowLoggedIn();
    } else {
        return nowLoggedOut();
    }
}, nowLoggedOut);

function activelyLogin() {
    return calli.getText("/?profile").then(readUserResource);
}

function readUserResource(doc) {
    var iri = /resource="([^" >]*)"/i.exec(doc);
    if (iri) {
        try {
            // now logged in
            window.localStorage.setItem("userIri", iri[1]);
        } catch(e) {}
    }
    return iri[1];
}

function nowLoggedIn() {
    var name = getUsername();
    if (name) {
        promiseUserName = calli.resolve(name);
        $(document.documentElement).addClass('login').removeClass('logout');
        try {
            window.localStorage.setItem("username", name);
        } catch(error) {}
        return name;
    } else {
        return nowLoggedOut();
    }
}

function nowLoggedOut() {
    promiseUserName = calli.resolve(null);
    $(document.documentElement).addClass('logout').removeClass('login');
    try {
        document.cookie = getUserCookieName() + '=;max-age=0';
    } catch (e) {}
    try {
        window.localStorage.removeItem('username');
        window.localStorage.removeItem('userIri');
        window.localStorage.removeItem("digestProfile");
        window.localStorage.removeItem("digestPassword");
    } catch(e) {}
    return null;
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

})(jQuery);

