// each-resource-in.js
/*
   Copyright (c) 2014 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

if (!window.calli) {
    window.calli = {};
}

window.calli.checkEachResourceIn = function(container) {
    var group = $(container);
    return function(element, index) {
        var el = typeof element == 'number' ? index : element;
        var resource = el.getAttribute('resource');
        group.find('[value="' + resource + '"]').prop('checked', true).change();
    };
};

window.calli.selectEachResourceIn = function(container) {
    var group = $(container);
    return function(element, index) {
        var el = typeof element == 'number' ? index : element;
        var resource = el.getAttribute('resource');
        group.find('[value="' + resource + '"]').prop('selected', true).closest('select').change();
    };
};

})(jQuery);

