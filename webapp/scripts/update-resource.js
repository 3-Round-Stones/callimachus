// update-resource.js
/*
   Copyright (c) 2014 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

calli.updateResource = function(event, rel) {
    return update(event, function(){
        if (rel) {
            this.removeAttribute('rel');
            this.removeAttribute('resource');
        } else {
            this.removeAttribute('about');
        }
    }, function(){
        if (rel && this.value) {
            this.setAttribute('rel', rel);
            this.setAttribute('resource', this.value);
        } else if (this.value) {
            this.setAttribute('about', this.value);
        }
    });
};

calli.updateProperty = function(event, property) {
    return update(event, function(){
        this.remoteAttr('property');
        this.remoteAttr('content');
    }, function(){
        if (this.value) {
            this.setAttribute('property', property);
            this.setAttribute('content', this.value);
        }
    });
};

function update(event, deselect, select) {
    var target = $(calli.fixEvent(event).target);
    var name = target.prop('name');
    var group = name ? $(document.getElementsByName(name)) : target;
    var checked = target.find('option:checked').addBack(':checked');
    var unchecked = target.find('option:not(:checked)').add(group.filter('option:not(:checked),:radio:not(:checked),:checkbox:not(:checked)'));
    var deselected = target.prop('value') ? unchecked : unchecked.addBack();
    var selected = target.prop('value') && target.is(':not(option):not(:radio):not(:checkbox)') ? checked.addBack() : checked;
    deselected.each(deselect);
    selected.each(select);
    return true;
}

})(jQuery, jQuery);

