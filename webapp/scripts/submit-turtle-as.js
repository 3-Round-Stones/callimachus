// submit-turtle-as.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.submitTurtleAs = function(event, fileName, create, folder) {
    var button = calli.fixEvent(event).target;
    var form = $(button).closest('form');
    var btn = $(button).filter('button');
    var resource = form.attr("about") || form.attr("resource") || '';
    var local = fileName || localPart(resource);
    btn.button('loading');
    return calli.promptForNewResource(folder, local).then(function(two){
        if (!two) return undefined;
        var url = two[0] + '?create=' + encodeURIComponent(create);
        var iri = two[0].replace(/\/?$/, '/') + two[1].replace(/%20/g, '+');
        form.attr("resource", iri);
        try {
            return calli.copyResourceData(form);
        } finally {
            if (resource) {
                form.attr("resource", resource);
            }
        }
    }).then(function(data){
        if (!data) return data;
        data.results.bindings.push({
            s: {type:'uri', value: data.head.link[0]},
            p: {type:'uri', value: 'http://purl.org/dc/terms/created'},
            o: {
                type:'literal',
                value: new Date().toISOString(),
                datatype: "http://www.w3.org/2001/XMLSchema#dateTime"
            }
        });
        return data;
    }).then(function(data){
        if (!data) return data;
        return calli.createTurtle(url, data);
    }).then(function(redirect){
        if (redirect) {
            window.location.href = redirect;
        } else {
            btn.button('reset');
        }
    }, function(error){
        btn.button('reset');
        return calli.error(error);
    });
};


function localPart(resource) {
    if (resource)
        return resource.replace(/.*\/(.+)/, '$1');
    return null;
}

})(jQuery);

