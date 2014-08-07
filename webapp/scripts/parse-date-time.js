// parse-date-time.js
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

calli.parseDateTime = function(element) {
    var el = element && typeof element == 'object' ? element : this;
    var datetime = el && el.getAttribute && el.getAttribute('datetime');
    var formatted = element && typeof element == 'string' ? element : datetime;
    return new Date(parseDateTime(formatted));
};

function parseDateTime(text, asof) {
    if (/^\s*([\d]+\-?){1,2}\s*$/.exec(text))
        return NaN;
    var struct = /(?:(\d{4})-?(\d{2})-?(\d{2}))?(?:[T ]?(\d{2}):?(\d{2}):?(\d{2})(?:\.(\d{3,}))?)?(?:(Z)|([+\-])(\d{2})(?::?(\d{2}))?)?/.exec(text);
    if (!struct)
        return Date.parse(text);
    var minutesOffset = 0;
    if (struct[8] !== 'Z' && struct[9]) {
        minutesOffset = +struct[10] * 60 + (+struct[11]);
        if (struct[9] === '+') {
            minutesOffset = 0 - minutesOffset;
        }
    }
    if (struct[1]) {
        if (struct[4] || struct[5] || struct[6]) {
            if (struct[8] || struct[9]) {
                return Date.UTC(+struct[1], +struct[2] - 1, +struct[3], +struct[4], +struct[5] + minutesOffset, +struct[6], 0);
            } else {
                return new Date(+struct[1], +struct[2] - 1, +struct[3], +struct[4], +struct[5], +struct[6], 0).getTime();
            }
        } else if (struct[8] || struct[9]) {
            return Date.UTC(+struct[1], +struct[2] - 1, +struct[3], 0, minutesOffset, 0, 0);
        } else {
            return new Date(+struct[1], +struct[2] - 1, +struct[3]).getTime();
        }
    } else if (struct[4] || struct[5] || struct[6]) {
        var now = asof || new Date();
        if (struct[8] || struct[9]) {
            return Date.UTC(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5] + minutesOffset, +struct[6], 0);
        } else {
            return new Date(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5], +struct[6], 0).getTime();
        }
    }
}

})(jQuery);

