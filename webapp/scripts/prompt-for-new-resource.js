// prompt-for-new-resource.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.promptForNewResource = function(container, localPart) {
    return calli.promise(function(callback){
        openSaveAsDialog(localPart, container, callback);
    });
};

function openSaveAsDialog(label, folder, callback) {
    var src = calli.getCallimachusUrl("pages/save-resource-as.html#");
    if (label) {
        src += encodeURIComponent(label.replace(/!/g,''));
    }
    if (folder) {
        src += '!' + folder + '?view';
    } else if (location.search.search(/\?create=/) === 0) {
        var page = calli.getPageUrl();
        src += '!' + page.substring(0, page.indexOf('?')) + '?view';
    } else {
        try {
            if (window.sessionStorage.getItem("LastFolder")) {
                src += '!' + window.sessionStorage.getItem("LastFolder");
            } else if (window.localStorage.setItem("LastFolder")) {
                src += '!' + window.localStorage.setItem("LastFolder");
            }
        } catch (e) {
            // ignore
        }
    }
    var called = false;
    var dialog = window.calli.openDialog(src, 'Save As...', {
        buttons: {
            "Save": function() {
                dialog.postMessage('GET label', '*');
            },
            "Cancel": function() {
                calli.closeDialog(dialog);
            }
        },
        onmessage: function(event) {
            var data = event.data;
            if (data == 'POST save') {
                dialog.postMessage('OK\n\n' + event.data, '*');
                dialog.postMessage('GET label', '*');
            } else if (data.indexOf('OK\n\nGET label\n\n') === 0) {
                label = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
                dialog.postMessage('GET resource', '*');
            } else if (data.indexOf('OK\n\nGET resource\n\n') === 0) {
                var ns = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
                var local = encodeURI(label).replace(/%25(\w\w)/g, '%$1');
                called = true;
                callback({container: ns, slug: local});
                calli.closeDialog(dialog);
            }
        },
        onclose: function() {
            if (!called){
                called = true;
                callback();
            }
        }
    });
    return dialog;
}

})(jQuery);

