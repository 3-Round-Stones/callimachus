// select-resoure.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

calli.selectFile = function(node, src) {
    return calli.selectResource(node, src);
};

calli.selectResource = function(event, src) {
    event = calli.fixEvent(event);
    var node = event.target;
    var list = $(node).filter('[dropzone]').add($(node).parents('[dropzone]'));
    if (!list.length)
        return true;
    var title = '';
    if (list.attr("id")) {
        title = $("label[for='" + list.attr("id") + "']").text();
    }
    if (!title) {
        title = list.find("label").text();
    }
    var dialog = null;
    var url = null;
    if (!src && $(node).attr('href')) {
        src = $(node).attr('href');
    }
    if (!src) {
        src = "/?view";
    }
    if (src.indexOf("/?view") >= 0 && window.sessionStorage) {
        try {
            var last = sessionStorage.getItem("LastFolder");
            if (last) {
                url = last;
            } else if (last = localStorage.getItem("LastFolder")) {
                url = last;
            }
        } catch (e) {
            // ignore
        }
    }
    var options = {
        onmessage: function(event) {
            if (event.data.indexOf('PUT src\n') == 0) {
                var data = event.data;
                src = data.substring(data.indexOf('\n\n') + 2);
            }
        },
        buttons: {
            "Select": function() {
                var uri = calli.listResourceIRIs(src)[0];
                var de = jQuery.Event('drop');
                de.dataTransfer = {getData:function(){return uri}};
                de.errorMessage = "Invalid Selection";
                $(node).trigger(de);
                calli.closeDialog(dialog);
            },
            "Cancel": function() {
                calli.closeDialog(dialog);
            }
        },
        onclose: function() {
            try {
                $(node)[0].focus();
            } catch (e) {
                // ignore
            }
        }
    };
    var openBrowseDialog = function(url) {
        dialog = calli.openDialog(url, title, options);
    };
    if (url) {
        jQuery.ajax({
            type:"GET",
            url:url,
            beforeSend: calli.withCredentials,
            complete:function(xhr) {
                if (xhr.status == 200 || xhr.status == 304) {
                    openBrowseDialog(url);
                } else {
                    openBrowseDialog(src);
                }
            }
        });
    } else {
        openBrowseDialog(src);
    }
    return false;
};

})(jQuery, jQuery);

