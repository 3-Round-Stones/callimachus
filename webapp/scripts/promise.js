// promise.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.promise = function(constructor) {
    return load.then(function(){
        return new self.Promise(constructor);
    });
};

calli.resolve = function(obj) {
    return load.then(function(){
        return self.Promise.resolve(obj);
    });
};

calli.reject = function(obj) {
    return load.then(function(){
        return self.Promise.reject(obj);
    });
};

calli.all = function(array) {
    return load.then(function(){
        return self.Promise.all(array);
    });
};

calli.sleep = function(milliseconds) {
    return load.then(function(){
        return new self.Promise(function(callback){
            setTimeout(callback, milliseconds);
        });
    });
};

var load = waitForPromise((typeof self.Promise == 'function' ? self.Promise.resolve.bind(self.Promise) : function(){
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
})());

var ready = load.then(function(){
    return new self.Promise(function(callback){
        $(function() {
            callback();
        });
    });
});

calli.ready = function(obj) {
    return ready.then(function(){
        return obj;
    });
};

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

var waiting;
function waitMore() {
    if (waiting) {
        waiting++;
    } else {
        $(document.documentElement).addClass("wait");
        waiting = 1;
    }
}
function waitLess() {
    waiting--;
    if (!waiting) {
        $(document.documentElement).removeClass("wait");
    }
}

function waitForPromise(promise) {
    waitMore();
    var p = promise.then(function(resolve){
        waitLess();
        return resolve;
    }, function(reject) {
        waitLess();
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
