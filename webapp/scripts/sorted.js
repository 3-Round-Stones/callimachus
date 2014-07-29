// sorted.js
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
    $(".asc,.desc", document).parents('.sorted').each(function() {
        sortElements(this);
    });
});

$(document).bind("DOMNodeInserted", handle);

function handle(event) {
    $(".asc,.desc", event.target).parents('.sorted').each(function(i, node) {
        sortElements(node);
    });
    if ($(event.target).is(".asc,.desc")) {
        sortElements($(event.target).parents('.sorted')[0]);
    }
}

function sortElements(node) {
    var nodes = node.childNodes;
    if (parseInt($(node).attr('data-sorted')) >= nodes.length)
        return;
    var list = $(nodes).get();
    var exclude = $(node).find(".sorted").find(".asc, .desc");
    list.sort(function(a, b) {
        if (a.nodeType < b.nodeType) return -1;
        if (a.nodeType > b.nodeType) return 1;
        if (a.nodeType != 1) return 0;
        var a1 = $(a).find(".asc").not(exclude).text();
        var a2 = $(b).find(".asc").not(exclude).text();
        try {
            var i1 = parseInt(a1);
            var i2 = parseInt(a2);
            if (i1 > i2) return 1;
            if (i1 < i2) return -1;
        } catch (e) {}
        if (a1 > a2) return 1;
        if (a1 < a2) return -1;
        var d1 = $(a).find(".desc").not(exclude).text();
        var d2 = $(b).find(".desc").not(exclude).text();
        try {
            var i1 = parseInt(d1);
            var i2 = parseInt(d2);
            if (i1 > i2) return -1;
            if (i1 < i2) return 1;
        } catch (e) {}
        if (d1 > d2) return -1;
        if (d1 < d2) return 1;
        return 0;
    });
    list = $(list).filter(function(){
        if (this.nodeType != 3 || this.data.search(/\S/) >= 0)
            return true;
        $(this).remove();
        return false;
    }).get();
    $(node).attr('data-sorted', list.length);
    $(list).each(function(i) {
        if (this != nodes[i]) {
            $(this).insertBefore(nodes[i]);
        }
    });
}

})(window.jQuery);
