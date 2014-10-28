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
    return function(element) {
        var el = element && typeof element == 'object' ? element : this;
        var resource = el.getAttribute('resource');
        var checkbox = group.find('[resource="' + resource + '"],[value="' + resource + '"]').first();
        return checkbox.prop('checked', true).change().get(0);
    };
};

window.calli.selectEachResourceIn = function(container) {
    var group = $(container);
    return function(element) {
        var el = element && typeof element == 'object' ? element : this;
        var resource = el.getAttribute('resource');
        var option = group.find('[resource="' + resource + '"],[value="' + resource + '"]').first();
        option.prop('selected', true).closest('select').change();
        return option.get(0);
    };
};

})(jQuery);

