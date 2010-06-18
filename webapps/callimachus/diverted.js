// diverted.js

var diverted = function(url, node) {
    var prefix = document.location.protocol + '//' + document.location.host + '/diverted;';
    if (url.indexOf(':') < 0) {
        if (node && node.baseURIObject && node.baseURIObject.resolve) {
            url = node.baseURIObject.resolve(url);
        } else {
            var a = document.createElement('a');
            a.setAttribute('href', url);
            if (a.href) {
                url = a.href;
            }
        }
    } else if (url.indexOf(prefix) == 0) {
    	return url;
    }
    if (url.lastIndexOf('?') > 0) {
        var uri = url.substring(0, url.lastIndexOf('?'));
        var qs = url.substring(url.lastIndexOf('?'));
        return prefix + encodeURIComponent(uri) + qs;
    }
    return prefix + encodeURIComponent(url) + '?view';
};

var divertLink = function(el) {
    document.location.href = diverted($(el).attr('href'), el);
};

var divertLinks = function() {
    $('a.diverted[href]').live('click', function(e) {
        e.preventDefault();
        divertLink(this);
        return false;
    });
};

$(document).ready(function() {
    divertLinks();
});
