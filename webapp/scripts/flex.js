// flex.js

(function($){

$(window).bind('resize', fillOutFlex);
$(window).load(function(event){
    $('iframe').load(fillOutFlex);
    $('img').load(fillOutFlex);
    fillOutFlex();
});

function fillOutFlex(e){
    var el = e ? e.target : window;
    // use timeout to reduce cpu stress during resize and eternal loops
    try {clearTimeout(window.flexTO);} catch (e) { }
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
    return $(parent).width() - margin;
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

