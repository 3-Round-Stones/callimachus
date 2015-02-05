// submit-form.js
/*
   Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.submitForm = function(event) {
    event = calli.fixEvent(event);
    event.preventDefault();
    var form = $(event.target).closest('form')[0];
    var finalTarget = form.target;
    var btn = $(form).find('button[type="submit"]');
    btn.button('loading');
    return calli.postForm(form).then(function(redirect){
        if (finalTarget && window.frames[finalTarget]) {
            window.frames[finalTarget].location.href = redirect + "?view";
        } else {
            if (window.parent != window && parent.postMessage) {
                parent.postMessage('POST resource\n\n' + redirect, '*');
            }
            window.location.href = redirect + "?view";
        }
    }, function(error) {
        btn.button('reset');
        return calli.error(error);
    });
};

calli.postForm = function(form) {
    return calli.resolve($(form)[0]).then(function(form){
        var iframe = document.createElement("iframe");
        iframe.name = newIframeName();
        iframe.style.display = "none";
        $('body').append(iframe);
        return calli.promise(function(resolve){
            var finalTarget = form.target;
            $(iframe).bind('load', function(event){
                var doc = iframe.contentDocument;
                if (doc.URL != "about:blank") {
                    resolve($(doc).text());
                }
            });
            form.target = iframe.name;
            calli.sleep(0).then(function(){
                // give chrome a chance to notice the new target value
                form.submit();
                form.target = finalTarget;
            });
        }).then(function(redirect){
            if (redirect && redirect.indexOf('http') === 0) {
                $(iframe).remove();
                return redirect;
            } else {
                var doc = iframe.contentDocument;
                var h1 = $(doc).find('h1.text-error').add($(doc).find('h1')).first().clone();
                var frag = document.createDocumentFragment();
                h1.contents().each(function() {
                    frag.appendChild(this);
                });
                var pre = $(doc).find('pre').text();
                $(iframe).remove();
                if (!pre) return calli.reject(frag);
                return calli.reject({message: frag, stack: pre});
            }
        });
    });
};

var iframe_counter = 0;
function newIframeName() {
    var iname = null;
    while (window.frames[iname = 'iframe-redirect-' + (++iframe_counter)]);
    return iname;
}

})(jQuery);
