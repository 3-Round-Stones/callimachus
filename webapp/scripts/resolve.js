// resolve.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli = window.calli || {};

var polyfill;

calli.resolve = function(obj) {
    if (typeof self.Promise == 'function')
        return waitForPromise(when(obj));
    var delayed = delayedPromise();
    if (!polyfill) {
        polyfill = $.ajax({
            url: calli.getCallimachusUrl("assets/promise-1.0.0.js"),
            dataType: "script",
            cache: true
        });
    }
    polyfill.done(function(){
        delayed._apply(when(obj));
    }).fail(function(xhr){
        delayed._apply(self.Promise.reject(xhr));
    });
    return waitForPromise(delayed);
};

calli.reject = function(obj) {
    if (typeof self.Promise == 'function')
        return waitForPromise(self.Promise.reject(obj));
    return calli.resolve().then(function(){
        return self.Promise.reject(obj);
    });
};

calli.all = function(array) {
    if (typeof self.Promise == 'function')
        return waitForPromise(self.Promise.all(array));
    return calli.resolve().then(function(){
        return self.Promise.all(array);
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
        "then": function() {
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

function waitForPromise(promise) {
    var waiting = calli.wait();
    var p = promise.then(function(resolve){
        waiting.over();
        return resolve;
    }, function(reject) {
        waiting.over();
        return self.Promise.reject(reject);
    });
    return {
        "then": function() {
            return waitForPromise(p.then.apply(promise, arguments));
        },
        "catch": function() {
            return waitForPromise(p["catch"].apply(promise, arguments));
        }
    };
}

})(jQuery);
