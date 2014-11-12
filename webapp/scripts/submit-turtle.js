// submit-turtle.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.submitTurtle = function(event, local) {
    event.preventDefault();
    var form = calli.fixEvent(event).target;
    var slug = encodeURI(local || '').replace(/%25(\w\w)/g, '%$1').replace(/%20/g, '+');
    var btn = $(form).find('button[type="submit"]');
    btn.button('loading');
    return calli.resolve(form).then(function(form){
        var action = calli.getFormAction(form);
        if (action.indexOf('?create=') > 0) {
            return calli.resolve(form).then(function(form){
                if (!local) return calli.copyResourceData(form);
                var previously = form.getAttribute("resource");
                var ns = window.location.pathname.replace(/\/?$/, '/');
                var resource = ns + slug;
                form.setAttribute("resource", resource);
                try {
                    return calli.copyResourceData(form);
                } finally {
                    if (previously) {
                        form.setAttribute("resource", previously);
                    }
                }
            }).then(function(data){
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
                return calli.postTurtle(action, data, {Slug: slug});
            });
        } else if (confirm("This create page is deprecated, use a different link next time, do you still want to continue")) {
            return calli.promptForNewResource(null, local).then(function(two){
                if (!two) return undefined;
                var folder = two.container, local = two.slug.replace(/%20/g, '+');
                var type = window.location.href.replace(/\?.*|\#.*/, '');
                var url = folder + '?create=' + encodeURIComponent(type);
                var resource = folder.replace(/\/?$/, '/') + local;
                form.setAttribute("resource", resource);
                return calli.postTurtle(url, calli.copyResourceData(form), {Slug: local});
            });
        } else {
            return undefined;
        }
    }).then(function(redirect){
        if (redirect) {
            if (window.parent != window && parent.postMessage) {
                parent.postMessage('POST resource\n\n' + redirect, '*');
            }
            window.location.replace(redirect);
        } else {
            btn.button('reset');
        }
    }, function(error){
        btn.button('reset');
        return calli.error(error);
    });
};

calli.postTurtle = function(url, data, headers) {
    var serializer = new TurtleSerializer();
    serializer.setNullUri(data.base);
    serializer.setMappings(data.prefix);
    data.results.bindings.forEach(function(triple){
        serializer.addTriple(triple);
    });
    return calli.postText(url, serializer.toString(), "text/turtle", headers);
};

})(jQuery);

