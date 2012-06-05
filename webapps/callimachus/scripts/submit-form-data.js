// submit-form-data.js
/*
   Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){

var iframe_counter = 0;
$('form[method="POST"][enctype="multipart/form-data"],form[method="POST"][enctype="application/x-www-form-urlencoded"]').submit(function(event) {
    var form = $(this);
    var resource = form.attr('about') || form.attr('resource');
    if (!resource || resource.indexOf(':') < 0 && resource.indexOf('/') != 0 && resource.indexOf('?') != 0)
        return true; // resource attribute not set yet
    if (!this.target || this.target.indexOf('iframe-redirect-') != 0) {
        var iname = null;
        while (window.frames[iname = 'iframe-redirect-' + (++iframe_counter)]);
        createIframeRedirect(iname, form.target);
        this.target = iname;
    }
    return true;
});

function createIframeRedirect(iname, finalTarget) {
    var iframe = $('<iframe></iframe>');
    iframe.attr('name', iname)
    iframe.bind('load', function(event) {
        var doc = this.contentWindow.document;
        if (doc.URL == "about:blank")
            return true;
        var redirect = $(doc).text();
        if (redirect && redirect.indexOf('http') == 0) {
            var event = $.Event("calliRedirect");
            event.location = window.calli.viewpage(redirect);
            $(this).trigger(event);
            if (!event.isDefaultPrevented()) {
                if (finalTarget && window.frames[finalTarget]) {
                    window.frames[finalTarget].location.href = event.location;
                } else {
                    if (window.parent != window && parent.postMessage) {
                        parent.postMessage('PUT src\n\n' + event.location, '*');
                    }
                    window.location.replace(event.location);
                }
            }
        } else {
            var h1 = $(doc).find('h1').clone();
            var frag = document.createDocumentFragment();
            h1.contents().each(function() {
                frag.appendChild(this);
            });
            var pre = $(doc).find('pre').text();
            calli.error(frag, pre);
        }
    });
    iframe.css('display', 'none');
    $('body').append(iframe);
    return iframe;
}

});
