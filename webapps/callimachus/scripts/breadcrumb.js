// breadcrumb.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function select(node, selector) {
    var set = $(node).find(selector).andSelf();
    set = set.add($(node).parents(selector));
    return set.filter(selector);
}

function handle(event) {
    breadcrumb(select(event.target, ".breadcrumb"));
}

function breadcrumb(element) {
    element.filter(':empty').each(function(i, node){
        var label = $(node).attr('href') || $('title').text();
        $(node).text(label.match(/\/([^\/]*)\/?$/)[1]);
    });
    if (location.pathname.indexOf('/diverted;') == 0) {
        element.each(function(i, node){
            $(node).mousedown(function() {
                if (!$(this).attr('resource')) {
                    var href = this.href;
                    $(this).attr('resource', href);
                    var diverted = '/diverted;' + encodeURIComponent(href);
                    diverted = diverted.replace(/%2F/g, '/').replace(/%3A/g, ':');
                    $(this).attr('href', diverted);
                }
            });
        });
    }
}

})(window.jQuery);

