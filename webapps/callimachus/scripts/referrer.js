// referrer.js

(function($){

function getPageLocationURL() {
	// window.location.href needlessly decodes URI-encoded characters in the URI path
	// https://bugs.webkit.org/show_bug.cgi?id=30225
	var path = location.pathname;
	if (path.match(/#/))
		return location.href.replace(path, path.replace('#', '%23'));
	return location.href;
}

try {
	/** Store the top previous page about a different resource */
	if (window.sessionStorage && !window.frameElement) {
		var here = ' ' + getPageLocationURL();
		if (here.indexOf('?') >= 0) {
			here = here.substring(0, here.indexOf('?'));
		}
		var referrer = document.referrer;
		var prev = sessionStorage.getItem("Previous");
		if (referrer && !(prev && prev.indexOf(here) > 0)) {
			sessionStorage.setItem("Previous", referrer + here);
		}
	}
} catch (e) { }

})(window.jQuery);

