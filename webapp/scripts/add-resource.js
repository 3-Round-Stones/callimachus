// add-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($,jQuery){

calli.addTemplate = calli.addResource = function(event, container) {
    event = calli.fixEvent(event);
    event.preventDefault();
    // container can be null, or a jQuery object, or a container node?
    var node = (container && (container.length || container.nodeType)) ? $(container) : $(event.target);
    var rel = node.closest('[data-add]');
    var add = rel.attr("data-add");
    if (!add)
        return true;
    return calli.getText(add, function(data) {
        var clone = $(data).clone();
        var child = clone.children("[about],[typeof],[typeof=''],[resource],[property]");
        if (!child.length) return; // child may be empty
        if (node.attr("data-add")) {
            node.append(child);
        } else {
            node.before(child);
        }
        child.find('input,textarea,select,button,a').addBack('input,textarea,select,button,a').first().focus();
        return child[0];
    }).then(undefined, calli.error);
};

})(jQuery, jQuery);

