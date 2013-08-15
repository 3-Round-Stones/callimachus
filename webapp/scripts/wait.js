// wait.js

(function($){

document.documentElement.className += ' wait';
var requestCount = 1;
var lastWait = 0;

var calli = window.calli || (window.calli={});

calli.wait = function() {
    requestCount++;
    $(document.documentElement).addClass("wait");
    return {over: function() {
        if (!this.closed) {
            this.closed = true;
            removeWait();
        }
    }};
};

function removeWait() {
    $(function() {
        setTimeout(function() {
            requestCount--;
            if (requestCount < 1) {
                var myWait = ++lastWait;
                setTimeout(function() {
                    if (myWait == lastWait && requestCount < 1) {
                        requestCount = 0;
                        $(document.documentElement).removeClass("wait");
                    }
                }, 400); // give browser a chance to draw the page
            }
        }, 0);
    });
}

$(window).load(removeWait);

$(window).bind("beforeunload", function() {
    requestCount++;
    $(document.documentElement).addClass("wait");
    setTimeout(removeWait, 1000);
});

$(window).bind("unload", function() {
    requestCount++;
    $(document.documentElement).addClass("wait");
});

$(document).ajaxSend(function(event, xhr, options){
    requestCount++;
    $(document.documentElement).addClass("wait");
});
$(document).ajaxComplete(removeWait);

})(window.jQuery);

