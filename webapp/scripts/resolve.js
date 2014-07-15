// resolve.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli = window.calli || {};

calli.resolve = function(obj) {
    if (typeof self.Promise == 'function') return when(obj);
    var delayed = delayedPromise();
    $.ajax({
        url: calli.getCallimachusUrl("assets/promise-1.0.0.js"),
        dataType: "script",
        cache: true
    }).done(function(){
        delayed._apply(when(obj));
    }).fail(function(xhr){
        delayed._apply(self.Promise.reject(xhr));
    });
};

function when(deferredPromise) {
    if (deferredPromise && typeof deferredPromise.done == 'function' && typeof deferredPromise.then == 'function') {
        return new self.Promise(function(resolve, reject){
            deferredPromise.then(resolve, reject);
        });
    }
    return self.Promise.resolve(deferredPromise);
}

function delayedPromise() {
    var calls = [];
    var promise;
    return {
        then: function() {
            if (promise) return promise.then.apply(promise, arguments);
            var delayed = delayedPromise();
            calls.push({
                name: "then",
                args: arguments,
                delayed: delayed
            });
            return delayed;
        },
        "catch": function() {
            if (promise) return promise["catch"].apply(promise, arguments);
            var delayed = delayedPromise();
            calls.push({
                name: "catch",
                args: arguments,
                delayed: delayed
            });
            return delayed;
        },
        _apply: function(context) {
            calls.forEach(function(call){
                call.delayed._apply(context[call.name].apply(context, call.args));
            });
            promise = context;
        }
    };
}

})(jQuery);
