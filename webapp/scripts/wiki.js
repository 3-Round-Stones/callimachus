// wiki.js
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

var parser = new creole();

var calli = window.calli = window.calli || {};

calli.parseCreole = function(element) {
    var pre = element && typeof element == 'object' ? element : this;
    var text = pre && pre.textContent || pre && pre.innerText || pre;
    var div = document.createElement('div');
    if (text && typeof text == 'string') {
        parser.parse(div, text);
        if (pre != text) {
            var attrs = pre.attributes;
            for(var j=attrs.length-1; j>=0; j--) {
                div.setAttribute(attrs[j].name, attrs[j].value);
            }
            div.setAttribute("content", text);
        }
    }
    return div;
};

$(document).ready(function() {
    $("pre.wiki", document).each(function() {
        initWiki(this);
    });
});
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
    $("pre.wiki", event.target).each(function(i, node) {
        initWiki(node);
    });
    if ($(event.target).is("pre.wiki")) {
        initWiki(event.target);
    }
}

function initWiki(pre) {
    var text = pre.textContent || pre.innerText;
    var div = document.createElement("div");
    var attrs = pre.attributes;
    for(var j=attrs.length-1; j>=0; j--) {
        div.setAttribute(attrs[j].name, attrs[j].value);
    }
    if (text && !div.getAttribute("content")) {
        div.setAttribute("content", text);
    }
    pre.parentNode.replaceChild(div, pre);
    if (text) {
        parser.parse(div, text);
    }
}

})(window.jQuery);

