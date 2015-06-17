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
    calli.promptForNewResource(folder, local).then(function(two){
        if (!two) return undefined;
        var url = two.container + '?create=' + encodeURIComponent(create);
        var iri = two.container.replace(/\/?$/, '/') + two.slug.replace(/%20/g, '+');
        return calli.resolve().then(function(){
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
            return calli.postTurtle(url, data, {Slug: local});
        });
    }).then(function(redirect){
        if (redirect) {
            window.location.href = redirect;
        } else {
            btn.button('reset');
        }
    }, function(error){
        btn.button('reset');
        calli.error(error);
    });
};


function localPart(resource) {
    if (resource)
        return resource.replace(/.*\/(.+)/, '$1');
    return null;
}

})(jQuery);

