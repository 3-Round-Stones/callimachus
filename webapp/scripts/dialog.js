// dialog.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

if (!window.calli) {
    window.calli = {};
}

function findFrameElement(iframe) {
    var elements = $('iframe').filter(function() {
        return iframe == this.contentWindow;
    });
    if (elements.length)
        return elements[0];
    return iframe.frameElement;
}

function asName(title) {
    if (!title)
        return "iframe";
    return title.toLowerCase().replace(/^\s+/,'').replace(/\s+$/,'').replace(/\s/g,'-').replace(/[^\-\w]/g,'_');
}

function asUniqueName(title) {
    var name = asName(title);
    if (!frames[name] || frames[name].name != name)
        return name;
    var i = 1;
    while (frames[name + i] && frames[name + i].name == name + i) {
        i++;
    }
    return name + i;
}

window.calli.closeDialog = function(iframe) {
    var frameElement = findFrameElement(iframe);
    $(frameElement).dialog('close');
}

$(window).on('resize', function(e) {
    resizeDialog(this);
});

/**
 * Resizes the given window's dialog DIV and associated IFRAME.
 */ 
function resizeDialog(win) {
    $(win.document).find('.ui-dialog').each(function() {
        var 
            oldDialogHeight = $(this).height(),
            oldIframeHeight = $(this).find('iframe').height(),
            newDialogHeight = $(this).height(0.9 * $(win).height()).height(),
            diff = newDialogHeight - oldDialogHeight,
            newIframeHeight = oldIframeHeight + diff
        ;
        $(this).find('iframe')
            .height(newIframeHeight)
            .dialog("option", "position", ['center', 'center']);
        ;
    });
}

window.calli.openDialog = function(url, title, options) {
    // init the dialog
    var settings = jQuery.extend({
        title: title,
        autoOpen: false,
        modal: false,
        draggable: true,
        resizable: false,
        autoResize: true,
        position: ['center', 'center'],
        width: '90%',
        height: 0.9 * $(window).height()
    }, options);
    var iframe = $("<iframe></iframe>");
    iframe.attr('src', 'about:blank');
    iframe.attr('name', asUniqueName(title));
    iframe.addClass('dialog');
    iframe.dialog(settings);
    // inter-window message processor
    var handle = function(event) {
        if (event.originalEvent.source == iframe[0].contentWindow) {
            var data = event.originalEvent.data;
            if (typeof settings.onmessage == 'function') {
                if (!event.source) {
                    event.source = event.originalEvent.source;
                }
                if (!event.data) {
                    event.data = event.originalEvent.data;
                }
                settings.onmessage(event);
            }
        }
    };
    $(window).bind('message', handle);
    // before close
    iframe.bind("dialogbeforeclose", function(event, ui) {
        var e = jQuery.Event("calliCloseDialog");
        var frameElement = findFrameElement(iframe[0].contentWindow);
        $(frameElement).trigger(e);
        return !e.isDefaultPrevented();
    });
    // close
    iframe.bind("dialogclose", function(event, ui) {
        $(window).unbind('message', handle);
        iframe.remove();
        iframe.parent().remove();
        if (typeof settings.onclose == 'function') {
            settings.onclose();
        }
    });
    // trigger open
    var e = jQuery.Event("calliOpenDialog");
    iframe.trigger(e);
    if (e.isDefaultPrevented()) {
        iframe.dialog('close');
        return null;
    } else {
        iframe.dialog("open");
        iframe.css('padding-left', '1em');
        iframe.css('width', '100%');
        iframe[0].src = url;
        if (typeof settings.onlookup == 'function') {
            var dialogTitle = iframe.parents(".ui-dialog").find(".ui-dialog-title");
            var form = $("<form></form>");
            var searchTerms = $("<input/>");
            searchTerms.attr("placeholder", "Lookup..");
            form.append(searchTerms);
            form.css('position', "absolute");
            form.css('top', dialogTitle.offset().top - iframe.parent().offset().top - 5);
            form.css('right', 30);
            iframe.before(form);
            form.submit(function(event) {
                event.preventDefault();
                if (searchTerms.val()) {
                    settings.onlookup(searchTerms.val());
                }
                return false;
            });
        }
        var win = iframe[0].contentWindow;
        try {
            win.close = function() { // not sure when this is called
                window.setTimeout(function() {
                    calli.closeDialog(win);
                }, 0);
            };
        } catch (e) {
            // use calli.closeDialog directly
        }
        return win;
    }
}

})(jQuery, jQuery);

