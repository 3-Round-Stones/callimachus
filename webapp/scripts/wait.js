// wait.js
/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

(function($){

document.documentElement.className += ' wait';
var requestCount = 1;
var lastWait = 0;

var calli = window.calli || (window.calli={});

calli.wait = function() {
    requestCount++;
    var closed = false;
    if (requestCount == 1) {
        $(document.documentElement).addClass("wait");
    }
    return {over: function() {
        if (!closed) {
            closed = true;
            removeWait();
        }
    }};
};

function removeWait() {
    requestCount--;
    if (requestCount < 1) {
        var myWait = ++lastWait;
        setTimeout(function() {
            if (myWait == lastWait && requestCount < 1) {
                requestCount = 0;
                $(document.documentElement).removeClass("wait");
            }
        });
    }
}

$(window).load(removeWait);

$(window).bind("beforeunload", function() {
    requestCount++;
    if (requestCount == 1) {
        $(document.documentElement).addClass("wait");
    }
    setTimeout(removeWait, 1000);
});

$(window).bind("unload", function() {
    requestCount++;
    if (requestCount == 1) {
        $(document.documentElement).addClass("wait");
    }
});

$(document).ajaxSend(function(event, xhr, options){
    requestCount++;
    if (requestCount == 1) {
        $(document.documentElement).addClass("wait");
    }
});
$(document).ajaxComplete(removeWait);

})(window.jQuery);

