// update-resource.js
/*
   Copyright (c) 2014 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.updateResource = function(event, rel) {
    return update(event, function(){
        this.removeAttribute('rel');
    }, function(){
        if (this.value && !this.getAttribute('rel')) {
            this.setAttribute('rel', rel);
        } else if (!this.value) {
            this.removeAttribute('rel');
        }
        if (this.tagName.match(/INPUT/i) && (this.value != "on" || !this.checked)) {
            this.setAttribute('resource', this.value);
        } else if (this.getAttribute('value')) {
            this.setAttribute('resource', this.getAttribute('value'));
        }
    });
};

calli.updateProperty = function(event, property) {
    return update(event, function(){
        this.removeAttribute('property');
    }, function(){
        if (this.value && !this.getAttribute('property')) {
            this.setAttribute('property', property);
        } else if (!this.value) {
            this.removeAttribute('property');
        }
        if (this.tagName.match(/INPUT/i) && (this.value != "on" || !this.checked)) {
            this.setAttribute('value', this.value);
        }
    });
};

function update(event, deselect, select) {
    var target = $(calli.fixEvent(event).target);
    var name = target.prop('name');
    var group = $(name ? document.getElementsByName(name) : []);
    var checked = target.find('option:checked').addBack(':checked');
    var unchecked = target.find('option:not(:checked)').add(group.filter(':radio:not(:checked),:checkbox:not(:checked)'));
    var deselected = target.prop('value') || target.is('select') ? unchecked : unchecked.add(target);
    var selected = target.prop('value') && !target.is('select,option,:radio,:checkbox') ? checked.add(target) : checked;
    deselected.each(deselect);
    selected.each(select);
    return true;
}

})(jQuery);

