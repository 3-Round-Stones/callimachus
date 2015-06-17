// add-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

calli.addResource = function(event, container) {
    event = calli.fixEvent(event);
    event.preventDefault();
    // container can be null, or a jQuery object, or a container node?
    var node = container ? $(container) : $(event.target.parentNode);
    var add = node.attr("data-add");
    if (!add)
        return true;
    calli.getText(add, function(data) {
        var clone = $(data).clone();
        var child = clone.children("[about],[typeof],[typeof=''],[resource],[property]");
        if (!child.length) return; // child may be empty
        if (container) {
            node.append(child);
        } else {
            node.before(child);
        }
        child.find('input,textarea,select,button,a').addBack('input,textarea,select,button,a').first().focus();
        return child[0];
    }).then(undefined, calli.error);
    return false;
};

})(jQuery);

