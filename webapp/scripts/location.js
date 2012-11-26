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

var bundle = $("script:last").attr("src");

window.calli.getCallimachusUrl = function(suffix) {
    var home = location.protocol + '//' + location.host;
    var link = $('link[rel="home"]');
    if (link[0] && link[0].href) {
        home = link[0].href;
        if (home.length - 1 == home.lastIndexOf('/')) {
            home = home.substring(0, home.length - 1);
        }
    }
    var base = home + '/callimachus/0.18/';
    if (bundle && bundle.indexOf('/scripts/') > 0) {
        base = bundle.substring(0, bundle.indexOf('/scripts/') + 1);
        home = base.substring(0, base.length - 1);
        while (home.lastIndexOf('/') == 0 || home.lastIndexOf('/') > home.indexOf('//') + 1) {
            home = home.substring(0, home.lastIndexOf('/'));
        }
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
    var url = calli.getPageUrl();
    if (url.indexOf('#') > 0)
        return url.substring(0, url.indexOf('#'));
    return url;
}

})(jQuery,jQuery);
