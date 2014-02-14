// input-date.js
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

$(document).ready(function() {
    select(document, "input").filter(function(){
        return this.getAttribute('type') == "date";
    }).each(function() {
        addDateSelect(this);
    });
});
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
    select(event.target, "input").filter(function(){
        return this.getAttribute('type') == "date";
    }).each(function(i, node) {
        addDateSelect(node);
    });
}

function select(node, selector) {
    return $(node).find(selector).andSelf().filter(selector);
}

function addDateSelect(node) {
    if (node.type == "text") {
        $(node).datepicker({dateFormat:'yy-mm-dd'});
    }
}

})(window.jQuery);
