// flex.js
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

$(document).ready(fillOutFlex);
$(window).bind('resize', fillOutFlex);
$(window).load(function(event){
    $('iframe').load(fillOutFlex);
    $('img').load(fillOutFlex);
    fillOutFlex();
});

var flexTO;
var flexWait;
function fillOutFlex(e){
    var el = e ? e.target : window;
    // use timeout to reduce cpu stress during resize and eternal loops
    if (flexTO) {
        clearTimeout(flexTO);
        flexTO = null;
    }
    if (!flexWait) {
        flexWait = calli.wait();
    }
    flexTO = window.setTimeout(function() {
        $('.flex').each(function() { flex(this); });
        flexWait.over();
        flexWait = null;
        flexTO = null;
    }, 50);
    return;
}

function flex(area) {
    $(area).css('height', getAvailableHeight(area));
}

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

if (window.parent != window) {
    $(window).bind('load', function() {
        setTimeout(function() {
            var innerHeight = window.innerHeight || document.documentElement.clientHeight;
            if (innerHeight < document.height) {
                parent.postMessage('PUT height\n\n' + document.height, '*');
            } else {
                var maxHeight = innerHeight;
                $('.flex').each(function() {
                    if (this.scrollHeight > this.clientHeight) {
                        var height = document.documentElement.scrollHeight + this.scrollHeight - this.clientHeight;
                        if (height > maxHeight) {
                            maxHeight = height;
                        }
                    }
                });
                if (maxHeight > innerHeight) {
                    parent.postMessage('PUT height\n\n' + maxHeight, '*');
                }
            }
            var clientWidth = document.documentElement.clientWidth;
            if (clientWidth < document.documentElement.scrollWidth) {
                parent.postMessage('PUT width\n\n' + document.documentElement.scrollWidth, '*');
            } else {
                var maxWidth = clientWidth;
                $('.flex').each(function() {
                    if (this.scrollWidth > this.clientWidth) {
                        var width = clientWidth + this.scrollWidth - this.clientWidth;
                        if (width > maxWidth) {
                            maxWidth = width;
                        }
                    }
                });
                if (maxWidth > clientWidth) {
                    parent.postMessage('PUT width\n\n' + maxWidth, '*');
                }
            }
        }, 0);
    });
    $(window).bind('message', function(event) {
        var source = event.originalEvent.source;
        var data = event.originalEvent.data;
        if (data.indexOf('PUT height\n\n') === 0) {
            $('iframe.flex').each(function() {
                if (this.contentWindow == source) {
                    var innerHeight = window.innerHeight || document.documentElement.clientHeight;
                    var height = parseInt(data.substring(data.indexOf('\n\n') + 2));
                    height += innerHeight - $(this).height();
                    parent.postMessage('PUT height\n\n' + height, '*');
                    this.contentWindow.postMessage('OK\n\nPUT height', '*');
                }
            });
        } else if (data.indexOf('PUT width\n\n') === 0) {
            $('iframe.flex').each(function() {
                if (this.contentWindow == source) {
                    var width = parseInt(data.substring(data.indexOf('\n\n') + 2));
                    width += document.documentElement.scrollWidth - $(this).width();
                    parent.postMessage('PUT width\n\n' + width, '*');
                    this.contentWindow.postMessage('OK\n\nPUT width', '*');
                }
            });
        }
    });
}

})(jQuery);

