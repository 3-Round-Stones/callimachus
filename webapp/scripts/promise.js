// promise.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.promise = function(constructor) {
    return polyfill.then(function(){
        return new self.Promise(constructor);
    });
};

calli.resolve = function(obj) {
    return polyfill.then(function(){
        return self.Promise.resolve(obj);
    });
};

calli.reject = function(obj) {
    return polyfill.then(function(){
        return self.Promise.reject(obj);
    });
};

calli.all = function(array) {
    return polyfill.then(function(){
        return self.Promise.all(array);
    });
};

calli.sleep = function(milliseconds) {
    return polyfill.then(function(){
        return new self.Promise(function(callback){
            setTimeout(callback, milliseconds);
        });
    });
};

var polyfill = (typeof self.Promise == 'function' ? self.Promise.resolve.bind(self.Promise) : function(){
    var delayed = delayedPromise();
    $.ajax({
        url: calli.getCallimachusUrl("assets/promise-1.0.0.js"),
        dataType: "script",
        cache: true
    }).done(function(){
        delayed._apply(self.Promise.resolve());
    }).fail(function(xhr){
        delayed._apply(self.Promise.reject(xhr));
    });
    return delayed;
})();

calli.ready = function(obj) {
    return ready.then(function(){
        return obj;
    });
};

calli.load = function(obj) {
    return load.then(function(){
        return obj;
    });
};

var ready = polyfill.then(function(){
    return new self.Promise(function(callback){
        $(function() {
            callback();
        });
    });
});

var loaded = false;
$(window).load(function(){
    loaded = true;
});

var load = polyfill.then(function(){
    return new self.Promise(function(callback){
        if (loaded) return callback();
        $(window).load(function() {
            callback();
        });
    });
});

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

})(jQuery);
