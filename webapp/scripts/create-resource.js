// create-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

calli.createResource = function(event, href) {
    event = calli.fixEvent(event);
    var node = event.target;
    if (!href && $(node).closest("[href]").length) {
        href = $(node).closest("[href]").attr("href");
    }
    var list = $(node).closest('[dropzone]');
    if (!href)
        return true;
    var title = '';
    if (!title && list.length && list.attr("id")) {
        title = $("label[for='" + list.attr("id") + "']").text();
    }
    if (!title && list.length) {
        title = list.find("label").text();
    }
    if (!title) {
        title = $(node).attr("title");
    }
    var options = {
        onmessage: function(event) {
            if (event.data.indexOf('PUT src\n') === 0) {
                var data = event.data;
                var src = data.substring(data.indexOf('\n\n') + 2);
                var uri = src.replace(/\?.*/,'');
                var de = jQuery.Event('drop');
                de.dataTransfer = {getData:function(){return uri;}};
                $(node).trigger(de);
            }
        },
        onclose: function() {
            list.unbind('calliLinked', onlinked);
            try {
                $(node)[0].focus();
            } catch (e) {
                // ignore
            }
        }
    };
    var dialog = calli.openDialog(href, title, options);
    var onlinked = function() {
        calli.closeDialog(dialog);
    };
    list.bind('calliLinked', onlinked);
    return false;
};

})(jQuery, jQuery);

