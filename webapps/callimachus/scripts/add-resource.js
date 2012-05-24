// add-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($,jQuery){

calli.addTemplate = calli.addResource = function(event, container) {
    event = calli.fixEvent(event);
    var node = container ? $(container) : $(event.target);
    var rel = node.closest('[data-add]');
    var add = rel.attr("data-add");
    if (!add)
        return true;
    jQuery.get(add, function(data) {
        var clone = $(data).clone();
        var child = clone.children("[about],[typeof],[typeof=''],[resource],[property]");
        if (node.attr("data-add")) {
            node.append(child);
        } else {
            node.before(child);
        }
        child.find(':input').andSelf().filter(':input:first').focus();
    }, 'text');
    return false;
};

})(jQuery, jQuery);

