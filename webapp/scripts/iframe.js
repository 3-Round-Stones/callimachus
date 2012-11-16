// iframe.js

(function($) {

    function checkWindowSize() {
        var innerHeight = window.innerHeight || document.documentElement.clientHeight;
        if (innerHeight < document.height) {
            parent.postMessage('PUT height\n\n' + document.height, '*');
        } else {
            var scrollHeight = document.height;
            $('#content').add($('#content').parents()).each(function(){
                scrollHeight += this.scrollHeight - this.clientHeight;
            });
            if (scrollHeight > innerHeight) {
                parent.postMessage('PUT height\n\n' + scrollHeight, '*');
            }
        }
        var clientWidth = document.documentElement.clientWidth;
        if (clientWidth < document.documentElement.scrollWidth) {
            parent.postMessage('PUT width\n\n' + document.documentElement.scrollWidth, '*');
        } else {
            var scrollWidth = document.documentElement.scrollWidth;
            $('#content').add($('#content').parents()).each(function(){
                scrollWidth += this.scrollWidth - this.clientWidth;
            });
            if (scrollWidth > clientWidth) {
                parent.postMessage('PUT height\n\n' + scrollWidth, '*');
            }
        }
    }

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
        var checkNumber = 0;
        $(window).bind('load resize DOMNodeInserted', function() {
            var lastCheck = ++checkNumber;
            setTimeout(function() {
                if (checkNumber == lastCheck) {
                    checkWindowSize();
                }
            }, 0);
        });
    }
})(jQuery);
