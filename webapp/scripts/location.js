// location.js

(function($,jQuery){

if (!window.calli) {
    window.calli = {};
}

window.calli.getPageUrl = function() {
    // window.location.href needlessly decodes URI-encoded characters in the URI path
    // https://bugs.webkit.org/show_bug.cgi?id=30225
    var path = location.pathname;
    if (path.match(/#/))
        return location.href.replace(path, path.replace('#', '%23'));
    return location.href;
};

window.calli.getCallimachusUrl = function(suffix) {
    var base = window.calli.baseURI;
    var home = base.substring(0, base.length - 1);
    while (home.lastIndexOf('/') == 0 || home.lastIndexOf('/') > home.indexOf('//') + 1) {
        home = home.substring(0, home.lastIndexOf('/'));
    }
    if (typeof suffix == 'string' && suffix.indexOf('/') == 0)
        return home + suffix;
    if (typeof suffix == 'string')
        return base + suffix;
    return base;
};

window.calli.getFormAction = function(form) {
    if (form.getAttribute("action"))
        return form.action;
    var url = window.calli.getPageUrl();
    if (url.indexOf('#') > 0)
        return url.substring(0, url.indexOf('#'));
    return url;
};

})(jQuery,jQuery);
