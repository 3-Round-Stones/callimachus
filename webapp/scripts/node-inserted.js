// node-inserted.js
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

jQuery(function($){

function isIE() {
    var msie = true;
    function notmsie() {
        msie = false;
    }
    $(document).bind("DOMNodeInserted", notmsie);
    var div = $("<div/>");
    div.appendTo($("body"));
    div.remove();
    return msie;
}

if (isIE()) {
    var _domManip = $.fn.domManip;
    var _remove = $.fn.remove;

    // 'append', 'prepend', 'after', 'before'
    $.fn.domManip = function(args, table, callback) {
        return _domManip.call(this, args, table, function(fragment) {
            var el = jQuery(fragment).children();
            var ret = callback.call(this, fragment);
            el.trigger("DOMNodeInserted");
            el.trigger("DOMSubtreeModified");
            return ret;
        });
    };
    $.fn.remove = function() {
        var ret = _remove.apply(this, arguments);
        var el = jQuery("*", this).add([this]);
        el.trigger("DOMNodeRemoved");
        el.trigger("DOMSubtreeModified");
        return ret;
    };
}

});

