// referrer.js

(function($){

try {
	/** Store the top previous page about a different resource */
	if (window.sessionStorage && window.parent == window) {
		var here = ' ' + calli.getPageURL();
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

