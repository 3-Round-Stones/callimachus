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
            var xhr = $.ajax({ type: "DELETE", url: url, beforeSend: function(xhr){
                var lastmod = getLastModified();
                if (lastmod) {
                    xhr.setRequestHeader("If-Unmodified-Since", lastmod);
                }
                withCredentials(xhr);
            }, success: function(data, textStatus) {
                try {
                    var event = jQuery.Event("calliRedirect");
                    event.location = redirect ? redirect : form.attr("data-redirect");
                    if (!event.location) {
                        if (!event.location) {
                            event.location = document.referrer;
                        }
                        if (event.location) {
                            var href = calli.getPageUrl();
                            if (href.indexOf('?') > 0) {
                                href = href.substring(0, href.indexOf('?'));
                            }
                            var referrer = event.location;
                            if (referrer.indexOf('?') > 0) {
                                referrer = referrer.substring(0, referrer.indexOf('?'));
                            }
                            if (href == referrer) {
                                event.location = null; // don't redirect back to self
                            }
                        }
                    }
                    if (event.location) {
                        // TODO verify this location is not going to 404 on us w/o causing an calliError
                    }
                    if (!event.location && location.pathname.match(/\//)) {
                        event.location = '../';
                    }
                    if (!event.location) {
                        event.location = './';
                    }
                    form.trigger(event)
                    if (!event.isDefaultPrevented()) {
                        if (event.location) {
                            location.replace(event.location);
                        } else {
                            location.replace('/');
                        }
                    }
                } catch(e) {
                    throw calli.error(e);
                }
            }})
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

function withCredentials(req) {
    try {
        req.withCredentials = true;
    } catch (e) {}
}

})(jQuery, jQuery);

