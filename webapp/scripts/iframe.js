// iframe.js

(function($) {
    if (window.parent != window) {
        document.documentElement.className += " iframe";
        var src = null;
        var postSource = function() {
            if (window.location.search == '?view' && parent.postMessage) {
                var url = calli.getPageUrl();
                if (url != src) {
                    src = url;
                    parent.postMessage('PUT src\n\n' + url, '*');
                }
            }
        }
        $(window).bind('popstate', postSource);
        $(window).bind('load', postSource);
    }
})(jQuery);
