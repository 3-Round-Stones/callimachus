// create-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

calli.createResource = function(event, href) {
    event = calli.fixEvent(event);
    event.preventDefault();
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
    return calli.promise(function(callback){
        var closed = false;
        var options = {
            onmessage: function(event) {
                if (event.data.indexOf('POST resource\n') === 0) {
                    var data = event.data;
                    var uri = data.substring(data.indexOf('\n\n') + 2);
                    var de = jQuery.Event('drop');
                    de.dataTransfer = {getData:function(){return uri;}};
                    $(node).trigger(de);
                    closed = true;
                    calli.closeDialog(dialog);
                    callback(uri);
                }
            },
            onclose: function() {
                try {
                    $(node)[0].focus();
                } catch (e) {
                    // ignore
                }
                if (!closed) callback();
            }
        };
        var dialog = calli.openDialog(href, title, options);
    });
};

})(jQuery, jQuery);

