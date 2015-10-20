// remove-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.removeResource = function(event) {
    event = calli.fixEvent(event);
    event.preventDefault();
    var node = event.target;
    var selector = "[about],[resource],[property],[typeof],[typeof='']";
    var pNodes = $(node).closest(selector);
    if (pNodes.length) {
        $(pNodes[0]).remove();
        return calli.resolve(pNodes[0]);
    }
    return calli.reject(new Error("No resource found"));
};

})(jQuery);

