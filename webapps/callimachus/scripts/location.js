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
    var home = location.protocol + '//' + location.host + '/';
    if (bundle && bundle.indexOf('/callimachus/') > 0) {
        home = bundle.substring(0, bundle.indexOf('/callimachus/') + 1);
    } else {
        var link = $('link[rel="home"]');
    	if (link[0] && link[0].href) {
    		home = link[0].href;
    	}
    }
	if (typeof suffix == 'string' && suffix.indexOf('/') == 0)
		return home + suffix.substring(1);
	if (typeof suffix == 'string')
		return home + 'callimachus/' + suffix;
	return home + 'callimachus/';
};

})(jQuery,jQuery);
