// insert-resource.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

calli.insertResource = function(event, container) {
    event = calli.fixEvent(event);
    var target = event.target;
    event.preventDefault();
    var text = event.dataTransfer.getData('URL');
    if (!text) {
        text = event.dataTransfer.getData("Text");
    }
    var errorMessage = event.errorMessage ? event.errorMessage : "Invalid Relationship";
    var script = container ? $(container) : $(event.target).find('[rel]').first();
    return calli.all(listResourceIRIs(text).map(function(iri) {
        return addSetItem(iri, script, errorMessage);
    })).then(undefined, calli.error);
};

function select(node, selector) {
    set = $(node).find(selector);
    if (set.length)
        return set[0];
    return $(node).closest('[dropzone]').find(selector)[0];
}

function listResourceIRIs(text) {
    var set = text ? text.replace(/\s+$/,"").replace(/^\s+/,"").replace(/\s+/g,'\n') : "";
    return set.split(/[^a-zA-Z0-9\-\._~%!\$\&'\(\)\*\+,;=:\/\?\#\[\]@]+/).filter(function(str) {
        if (!str || str.indexOf('_:') >= 0)
            return false;
        return str.indexOf(':') >= 0 || str.indexOf('/') >= 0;
    }).map(function(url) {
        if (url.indexOf('?view') >= 0) {
            return url.substring(0, url.indexOf('?view'));
        }
        return url.substring(0);
    });
}

function addSetItem(uri, script, errorMessage) {
    var url = script.attr("data-construct").replace("{resource}", encodeURIComponent(uri));
    return calli.getText(url).then(function(data) {
        var input = data ? $(data).children("[about],[resource]") : data;
        if (input && input.length) {
            script.append(input);
            return input[0];
        } else if (errorMessage) {
            return calli.reject(errorMessage);
        }
    });
}

})(jQuery, jQuery);

