// location.js

(function($,jQuery){

if (!window.calli) {
	window.calli = {};
}

window.calli.getPageURL = function() {
	// window.location.href needlessly decodes URI-encoded characters in the URI path
	// https://bugs.webkit.org/show_bug.cgi?id=30225
	var path = location.pathname;
	if (path.match(/#/))
		return location.href.replace(path, path.replace('#', '%23'));
	return location.href;
};

window.calli.getCallimachusURL = function(suffix) {
	var home = $('link[rel="home"]')[0].href;
	if (!home) {
		home = location.protocol + '//' + location.host + '/';
	}
	if (typeof suffix == 'string' && suffix.indexOf('/') == 0)
		return home + suffix.substring(1);
	if (typeof suffix == 'string')
		return home + 'callimachus/' + suffix;
	return home + 'callimachus/';
};

})(jQuery,jQuery);
