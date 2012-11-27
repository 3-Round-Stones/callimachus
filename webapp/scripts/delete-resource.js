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
    event = calli.fixEvent(event);
    var form = $(event.target);
    if(!form.is('form')) form = form.closest('form');

    if (event && !confirm("Are you sure you want to delete " + document.title + "?"))
        return;
    
    if (!form || !form.length) {
        form = $("form[about],form[resource]");
    }
    if (!form.length) {
        form = $(document);
    }
    try {
        var de = jQuery.Event("calliDelete");
        form.trigger(de);
        if (!de.isDefaultPrevented()) {
            var url = calli.getPageUrl();
            if (url.indexOf('?') > 0) {
                url = url.substring(0, url.indexOf('?'));
            }
            var xhr = $.ajax({ type: "DELETE", url: url, dataType: "text", beforeSend: function(xhr){
                var lastmod = getLastModified();
                if (lastmod) {
                    xhr.setRequestHeader("If-Unmodified-Since", lastmod);
                }
                calli.withCredentials(xhr);
            }, complete: function(xhr) {
                try {
                    if (xhr.status >= 200) {
                        var event = jQuery.Event("calliRedirect");
                        event.location = redirect ? redirect : xhr.getResponseHeader('Location');
                        if (!event.location && location.pathname.match(/\/$/)) {
                            event.location = '../';
                        } else if (!event.location) {
                            event.location = './';
                        }
                        form.trigger(event)
                        if (!event.isDefaultPrevented()) {
                            if (event.location) {
                                window.location.replace(event.location);
                            } else {
                                window.location.replace('/');
                            }
                        }
                    }
                } catch(e) {
                    throw calli.error(e);
                }
            }});
        }
    } catch(e) {
        calli.error(e);
    }
}

function getLastModified() {
    try {
        var committedOn = $('#resource-lastmod').find('[property=audit:committedOn]').attr('content');
        return new Date(committedOn).toGMTString();
    } catch (e) {
        return null;
    }
}

})(jQuery, jQuery);

