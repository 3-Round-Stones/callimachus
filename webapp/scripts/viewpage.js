// viewpage.js

(function($){

$(document).ready(function() {
    handle({target: document});// fake event parameter
});
$(document).bind("DOMNodeInserted", handle);

function select(node, selector) {
    return $(node).find(selector).andSelf().filter(selector);
}

function handle(event) {
    select(event.target, "a.view").each(function() {
        var href = window.calli.viewpage(this.href);
        var link = $(this);
        if (this.href != href) {
            var resource = link.attr("href");
            $(this).mousedown(function() {
                if (!link.attr("resource")) {
                    link.attr("resource", resource);
                }
                this.href = href;
            });
            $(this).bind('dragstart', function(event) {
                var e = event.originalEvent;
                e.dataTransfer.setData('text/uri-list', resource);
                e.dataTransfer.setData('text/plain', resource);
            });
            link.addClass("view");
        } else {
            link.removeClass("view");
        }
    });
    select(event.target, "a.diverted,a[data-diverted]").each(function() {
        var qry = this.getAttribute("data-diverted");
        if (typeof qry == 'string') {
            var href = window.calli.diverted(this.href, qry);
        } else if (this.href.indexOf('?') >= 0) {
            qry = this.href.substring(this.href.indexOf('?') + 1);
            var href = this.href.substring(0, this.href.indexOf('?'));
            href = window.calli.diverted(href, qry);
        } else {
            var href = window.calli.diverted(this.href);
        }
        var link = $(this);
        if (this.href != href) {
            var resource = link.attr("href");
            $(this).mousedown(function() {
                if (!link.attr("resource")) {
                    link.attr("resource", resource);
                }
                this.href = href;
            });
            $(this).bind('dragstart', function(event) {
                var e = event.originalEvent;
                e.dataTransfer.setData('text/uri-list', resource);
                e.dataTransfer.setData('text/plain', resource);
            });
        }
        link.removeAttr("data-diverted");
        link.removeClass("diverted");
    });
}

if (!window.calli) {
    window.calli = {};
}

window.calli.viewpage = function(uri) {
    var url = calli.listResourceIRIs(uri)[0];
    if (url.indexOf(':') < 0) {
        if (document.baseURIObject && document.baseURIObject.resolve) {
            url = document.baseURIObject.resolve(url);
        } else {
            var a = document.createElement('a');
            a.setAttribute('href', url);
            if (a.href) {
                url = a.href;
            }
        }
    }
    var prefix = window.location.origin + '/';
    if (url.indexOf(prefix) == 0) {
        url = url.substring(prefix.length - 1);
    }
    return prefix + '?go=' + encodeURIComponent(url).replace(/%2F/g, '/').replace(/%3A/g, ':');
}

window.calli.diverted = function(uri, query) {
    var url = uri;
    if (window.location.pathname.indexOf('/diverted;') == 0) {
        if (url.indexOf(':') < 0) {
            if (document.baseURIObject && document.baseURIObject.resolve) {
                url = document.baseURIObject.resolve(url);
            } else {
                var a = document.createElement('a');
                a.setAttribute('href', url);
                if (a.href) {
                    url = a.href;
                }
            }
        }
        if (url.indexOf(window.location.origin) != 0) {
            var prefix = window.location.origin + '/diverted;';
            url = prefix + encodeURIComponent(url).replace(/%2F/g, '/').replace(/%3A/g, ':');
        }
    }
    if (typeof query == "string") {
        var frag = "";
        if (url.indexOf('#') > 0) {
            frag = url.substring(url.indexOf('#'));
            url = url.substring(0, url.indexOf('#'));
        }
        if (url.indexOf('?') > 0) {
            url = url.substring(0, url.indexOf('?'));
        }
        url = url + '?' + query + frag;
    }
    return url;
}

})(window.jQuery);

