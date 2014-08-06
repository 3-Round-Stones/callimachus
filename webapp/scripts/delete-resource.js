// delete-resource.js
/*
   Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

if (!window.calli) {
    window.calli = {};
}
window.calli.deleteResource = function(event, redirect) {
    try {
        var form = $(calli.fixEvent(event).target).closest('form');
    
        if (event && event.type) {
            var prompt = event.message;
            if (typeof prompt === "undefined") {
                prompt = "Are you sure you want to delete " + document.title + "?";
            }
            if (prompt && !confirm(prompt))
                return;
        }

        var url = calli.getPageUrl();
        if (form.length) {
            url = calli.getFormAction(form[0]);
        } else {
            form = $(document);
        }
        var de = jQuery.Event("calliDelete");
        de.location = event.location || url;
        de.resource = event.resource || de.location.replace(/\?.*/,'');
        form.trigger(de);
        if (!de.isDefaultPrevented()) {
            var xhr = $.ajax({ type: "DELETE", url: de.location, dataType: "text", xhrFields: {withCredentials: true}, beforeSend: function(xhr){
                var lastmod = calli.lastModified(de.location);
                if (lastmod) {
                    xhr.setRequestHeader("If-Unmodified-Since", lastmod);
                }
            }});
            calli.resolve(xhr).then(function(responseText) {
                var event = jQuery.Event("calliRedirect");
                event.cause = de;
                event.resource = de.resource;
                event.location = redirect;
                var contentType = xhr.getResponseHeader('Content-Type');
                if (!event.location && contentType !== null && contentType.indexOf('text/uri-list') === 0) {
                    event.location = xhr.responseText;
                }
                if (!event.location && window.location.pathname.match(/\/$/)) {
                    event.location = '../';
                } else if (!event.location) {
                    event.location = './';
                }
                form.trigger(event);
                if (!event.isDefaultPrevented()) {
                    if (event.location) {
                        window.location.replace(event.location);
                    } else {
                        window.location.replace('/');
                    }
                }
            }).then(undefined, calli.error);
        }
    } catch(e) {
        calli.error(e);
    }
};

})(jQuery, jQuery);

