// compare-elements-by.js
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

var calli = window.calli || (window.calli={});

calli.compareElementsBy = function(valueOf) {
    var val = typeof valueOf == 'function' ? valueOf : typeof valueOf == 'string' ? function iterator(element) {
        var text = $(element).find(valueOf).text();
        var int = parseInt(text, 10);
        if (isNaN(int))
            return text;
        return int;
    } : function(element) {
        var text = $(element).text();
        var int = parseInt(text, 10);
        if (isNaN(int))
            return text;
        return int;
    };
    return function(a,b) {
        var v1 = val(a);
        var v2 = val(b);
        if (v1 == v2) return 0;
        if (v1 < v2) return -1;
        if (v1 > v2) return 1;
        // as strings
        if ((''+v1) < (''+v2)) return -1;
        if ((''+v1) > (''+v2)) return 1;
        // as JSON
        if (JSON.stringify(v1) < JSON.stringify(v2)) return -1;
        if (JSON.stringify(v1) > JSON.stringify(v2)) return 1;
        return 0;
    };
};

})(jQuery);
