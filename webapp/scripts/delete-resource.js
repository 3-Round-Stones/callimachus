// delete-resource.js
/*
   Copyright (c) 2011-2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.deleteResource = function(event) {
    if (!confirm("Are you sure you want to delete " + document.title + "?"))
        return;
    var btn = $(calli.fixEvent(event).target);
    btn.button('loading');
    calli.resolve(btn).then(function(btn){
        return btn.closest('form')[0];
    }).then(function(form){
        return calli.getFormAction(form);
    }).then(function(url){
        return calli.deleteText(url);
    }).then(function(redirect){
        window.location.replace(redirect);
    }, function(error){
        btn.button('reset');
        calli.error(error);
    });
};

})(jQuery);

