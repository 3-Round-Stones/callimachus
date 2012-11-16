// sorted.js

(function($){

$(document).ready(function() {
    $(".asc,.desc", document).parents('.sorted').each(function() {
        sortElements(this);
    })
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

