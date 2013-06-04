// etag.js

window.calli = window.calli || {};

try {
    window.calli.etag = function(url, value) {
        var uri = url;
        if (uri.indexOf('http') != 0) {
            if (document.baseURIObject && document.baseURIObject.resolve) {
                uri = document.baseURIObject.resolve(uri);
            } else {
                var a = document.createElement('a');
                a.setAttribute('href', uri);
                if (a.href) {
                    uri = a.href;
                }
            }
        }
        if (uri.indexOf('?') > 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }
        if (value) {
            return window.sessionStorage.setItem(uri + " ETag", value);
        } else {
            return window.sessionStorage.getItem(uri + " ETag");
        }
    };
    if (!window.sessionStorage) {
        window.calli.etag = function(){return null;};
    }
} catch(e) {
    window.calli.etag = function(){return null;};
}
