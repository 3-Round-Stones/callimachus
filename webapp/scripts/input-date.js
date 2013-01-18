// input-date.js

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
