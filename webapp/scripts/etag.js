// etag.js

window.calli = window.calli || {};

if (window.sessionStorage) {
    window.calli.etag = function(url, value) {
        var uri = url;
        if (uri.indexOf('?') > 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }
        if (value) {
            return window.sessionStorage.setItem(uri + " ETag", value);
        } else {
            return window.sessionStorage.getItem(uri + " ETag");
        }
    };
} else {
    window.calli.etag = function(){return null;};
}
