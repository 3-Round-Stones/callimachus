// fill-element.js
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

calli.fillElement = function(element) {
    var flexElement = function(){
        $(element).css('display', 'block').css('width', '100%').css('height', getAvailableHeight(element));
    };
    $(window).bind('resize', flexElement);
    flexElement();
    return calli.resolve().then(flexElement);
};

function getAvailableHeight(area) {
    var innerHeight = $(area).height();
    var clientHeight = window.innerHeight || document.documentElement.clientHeight;

    var container = $(area).parents('form');
    if (!container.length) {
        container = $(area).parents('.container');
        if (!container.length) {
            container = $(area).parents('body>*');
        }
    }
    var form = bottom(container) - innerHeight;
    if (form > 0 && form <= clientHeight / 3)
        return clientHeight - form;
    var top = $(area).offset().top;
    if (top <= clientHeight / 3)
        return clientHeight - top;
    var formHeight = container.outerHeight(true) - innerHeight;
    if (formHeight > 0 && formHeight <= clientHeight / 3)
        return clientHeight - formHeight;
    return clientHeight;
}

function bottom(element) {
    if ($(element).length)
        return $(element).offset().top + $(element).outerHeight(true);
    return null;
}

})(jQuery);

