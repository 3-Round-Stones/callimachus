// flash.js
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

var unloading = false;
$(window).unload(function() {
    unloading = true;
}).bind('beforeunload', function(){
    unloading = true;
});

$(document).error(function(event) {
    if (event.message && !event.isDefaultPrevented()) {
        if (flash(event.message, event.stack)) {
            event.stopPropagation();
        }
    }
});

function flash(message, stack) {
    var msg = $("#calli-error");
    var template = $('#calli-error-template').children();
    if (!msg.length || !template.length)
        return false;
    var widget = template.clone();
    widget.append(message);
    if (stack) {
        var pre = $("<pre/>");
        pre.append(stack);
        pre.hide();
        var more = $('<a/>');
        more.text("»");
        more.click(function() {
            pre.toggle();
        });
        widget.append(more);
        widget.append(pre);
    }
    setTimeout(function() {
        if (!unloading) {
            msg.append(widget);
            scroll(0,0);
        }
    }, 0);
    return true;
}

})(window.jQuery);

