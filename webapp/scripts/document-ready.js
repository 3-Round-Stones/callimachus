/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var currentlyLoading = 0;

$(document).ready(function () {
    calliReady();
});

$(document).ajaxSend(function(event, xhr, options){
    currentlyLoading++;
});

$(document).ajaxComplete(function(event, xhr, options){
    currentlyLoading--;
    calliReady();
});

function calliReady(node) {
    if (!currentlyLoading) {
        setTimeout(function() {
            // wait until all other events have fired
            if (!currentlyLoading) {
                currentlyLoading++; // don't run this again
                $(document).trigger("calliReady");
            }
        }, 0);
    }
}

})(jQuery);

