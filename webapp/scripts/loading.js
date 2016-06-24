// loading.js
/*
   Copyright (c) 2015 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

var timeout;

calli.loading = function(target, callback) {
    var $target = $(target);
    if ($target.is('form')) {
        var submit = $target.find('button[type="submit"]');
        if (submit.length) return calli.loading(submit, callback);
    }
    $target.addClass("loading disabled").attr("disabled", "disabled");
    var selector = 'i.glyphicon.glyphicon-refresh';
    var prepend = $('<i class="glyphicon glyphicon-refresh"></i>').add(document.createTextNode(' '));
    if (timeout === undefined && $target.is("a,button")) timeout = self.setTimeout(function(){
        if ($target.is(".loading") && !$target.children(selector).length) {
            $target.prepend(prepend);
        }
        timeout = undefined;
    }, 1000);
    return function() {
        if (timeout !== undefined) {
            self.clearTimeout(timeout);
            timeout = undefined;
        }
        $target.children(selector).remove();
        $target.contents().each(function(){
            if (!this.previousSibling && this.nodeValue == ' ' && this.nodeType == 3) {
                this.parentNode.removeChild(this);
            }
        });
        $target.removeClass("loading disabled").removeAttr("disabled");
        if (callback) return callback.apply(this, arguments);
    };
};

})(jQuery);
