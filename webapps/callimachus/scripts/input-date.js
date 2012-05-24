// input-date.js

(function($){

$(document).ready(handle);
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
    $(node).datepicker({dateFormat:'yy-mm-dd'});
}

})(window.jQuery);
