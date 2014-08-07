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
        if (!this.getAttribute('rel')) {
            this.setAttribute('rel', rel);
        }
        if (this.value && (this.value != "on" || !this.checked)) {
            this.setAttribute('resource', this.value);
        }
    });
};

calli.updateProperty = function(event, property) {
    return update(event, function(){
        this.removeAttribute('property');
    }, function(){
        if (!this.getAttribute('property')) {
            this.setAttribute('property', property);
        }
        if (this.value && (this.value != "on" || !this.checked)) {
            this.setAttribute('content', this.value);
        }
        if (this.getAttributeNode('value')) {
            this.removeAttribute('value');
        }
    });
};

function update(event, deselect, select) {
    var target = $(calli.fixEvent(event).target);
    var name = target.prop('name');
    var group = name ? $(document.getElementsByName(name)) : target;
    var checked = target.find('option:checked').addBack(':checked');
    var unchecked = target.find('option:not(:checked)').add(group.filter('option:not(:checked),:radio:not(:checked),:checkbox:not(:checked)'));
    var deselected = target.prop('value') || target.is('select') ? unchecked : unchecked.add(target);
    var selected = target.prop('value') && !target.is('select,option,:radio,:checkbox') ? checked.add(target) : checked;
    deselected.each(deselect);
    selected.each(select);
    return true;
}

})(jQuery);

