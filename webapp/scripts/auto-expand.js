// auto-expand.js

(function($){

$(window).bind('resize', fillOutTextArea);
$(document).bind('change', findAutoExpandTextArea);
$(document).bind('keypress', findAutoExpandTextArea);
$(document).bind('input', findAutoExpandTextArea);
$(document).bind('paste', findAutoExpandTextArea);
$(document).bind("DOMNodeInserted", findAutoExpandTextArea);
$(document).ready(function(){setTimeout(fillOutTextArea, 0)});
$(window).load(function(event){
    fillOutTextArea();
});

function fillOutTextArea(){
    findAutoExpandTextAreaIn(document);
}

function findAutoExpandTextArea(event) {
    findAutoExpandTextAreaIn(event.target);
}

function findAutoExpandTextAreaIn(target) {
    var areas = $(".auto-expand", target);
    if ($(target).is(".auto-expand")) {
        areas = areas.add(target);
    }
    for (var i = 0; i < areas.length; i++) {
        expand(areas[i]);
    }
    $(areas).unbind("paste", targetAutoExpandTextArea);
    $(areas).bind("paste", targetAutoExpandTextArea);
}

function targetAutoExpandTextArea(event) {
    if (event.target && event.target.className.match(/\bauto-expand\b/)) {
        setTimeout(function(){
            expand(event.target);
        }, 0);
    }
}

function expand(area) {
    var contentWidth = getAvailableWidth(area);
    var contentHeight = getAvailableHeight(area);
    if ($(area).is(":input")) {
        expandTextArea(area, contentWidth, contentHeight);
    }
}

function expandTextArea(area, contentWidth, innerHeight) {
    var width = area.cols || area.size;
    var height = area.rows;
    var maxCols = Math.min(Math.floor(contentWidth / area.offsetWidth * width - 3), Math.floor(contentWidth / 8 - 3));
    var maxRows = Math.floor(innerHeight / area.offsetHeight * height - 3);
    if (!maxCols || maxCols < 20) {
        maxCols = 96;
    }
    if (!maxRows || maxRows < 3) {
        maxRows = 43;
    }
    var lines = area.value.split("\n");
    var cols = 20;
    var rows = Math.max(1, lines.length);
    for (var i = 0; i < lines.length; i++) {
        var len = lines[i].replace(/\t/g, "        ").length;
        if (cols < len + 1) {
            cols = len + 1;
        }
        rows += Math.floor(len / maxCols);
    }
    if (area.type == "textarea") {
        area.cols = Math.min(maxCols, cols + 1);
        area.rows = Math.min(maxRows, rows + 1);
    } else {
        area.size = Math.min(maxCols, cols + 1);
    }
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

function getAvailableWidth(area) {
    var parent = getParentBlock(area);
    var margin = $(area).outerWidth(true) - $(area).width();
    var breakFlag = false;
    $(area).parents().each(function(){
        if (this == parent) {
            breakFlag = true;
        } else if (!breakFlag) {
            margin += $(this).outerWidth(true) - $(this).width();
        }
    });
    var asideLeft = getAsideLeft(area);
    if ($(parent).offset().left + $(parent).outerWidth(true) <= asideLeft)
        return $(parent).width() - margin;
    return $(parent).width() - margin - $(parent).offset().left - $(parent).outerWidth(true) + asideLeft;
}

function getParentBlock(area) {
    var parent = null;
    var parents = $(area).parents();
    for (var i=0;i<parents.length;i++) {
        parent = parents[i];
        var floatStyle = $(parents[i]).css('float');
        if (floatStyle && floatStyle != 'none')
            break;
        if ($(parents[i]).css('display') == 'block')
            break;
    }
    if (parent)
        return parent;
    return $('body')[0];
}

function getAsideLeft(area) {
    var clientWidth = document.documentElement.clientWidth;
    var asideLeft = clientWidth;
    var areaTop = $(area).offset().top;
    $("#sidebar,.aside:visible,aside:visible").filter(function(){
        var top = $(this).offset().top;
        return areaTop < top + $(this).outerHeight(true) && areaTop + $(area).outerHeight(true) > top;
    }).each(function() {
        var left = $(this).offset().left;
        left -= parsePixel($(this).css("margin-left"));
        if (left < asideLeft) {
            asideLeft = left;
        }
    });
    return asideLeft;
}

function parsePixel(str) {
    if (!str)
        return 0;
    if (str.indexOf('px') > 0)
        return parseInt(str.substring(0, str.indexOf('px')));
    if (str == "thin")
        return 1;
    if (str == "medium")
        return 3;
    if (str == "thick")
        return 5;
    return 0;
}

$(window).bind('resize', fillOutFlex);
$(window).load(function(event){
    $('iframe').load(fillOutFlex);
    $('img').load(fillOutFlex);
    fillOutFlex();
});

function fillOutFlex(e){
    var el = e ? e.target : window;
    // use timeout to reduce cpu stress during resize and eternal loops
    try {clearTimeout(window.flexTO)} catch (e) { }
    window.flexTO = window.setTimeout(function() {
        $('.flex').each(function() { flex(this); });
    }, 50);
    return;
}

function flex(area) {
    var contentWidth = getAvailableWidth(area);
    var innerHeight = getAvailableHeight(area);
    $(area).css('width', contentWidth);
    $(area).css('height', innerHeight);
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
        if (data.indexOf('PUT height\n\n') == 0) {
            $('iframe.flex').each(function() {
                if (this.contentWindow == source) {
                    var innerHeight = window.innerHeight || document.documentElement.clientHeight;
                    var height = parseInt(data.substring(data.indexOf('\n\n') + 2));
                    height += innerHeight - $(this).height();
                    parent.postMessage('PUT height\n\n' + height, '*');
                    this.contentWindow.postMessage('OK\n\nPUT height', '*');
                }
            });
        } else if (data.indexOf('PUT width\n\n') == 0) {
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

