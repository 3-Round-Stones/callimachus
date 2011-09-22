// iframe.js

(function($) {

	function getPageLocationURL() {
		// window.location.href needlessly decodes URI-encoded characters in the URI path
		// https://bugs.webkit.org/show_bug.cgi?id=30225
		var path = location.pathname;
		if (path.match(/#/))
			return location.href.replace(path, path.replace('#', '%23'));
		return location.href;
	}

	if (window.frameElement) {
		document.documentElement.className += " iframe";
		var src = null;
		var postSource = function() {
			if (window.location.search == '?view' && parent.postMessage) {
				var url = getPageLocationURL();
				if (url != src) {
					src = url;
					parent.postMessage('PUT src\n\n' + url, '*');
				}
			}
		}
		$(window).bind('popstate', postSource);
		$(postSource);
	}
})(jQuery);
