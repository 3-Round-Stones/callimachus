// submit-turtle.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli = window.calli || {};

calli.submitTurtle = function(event, local) {
    event.preventDefault();
    var form = calli.fixEvent(event).target;
    var btn = $(form).find('button[type="submit"]');
    btn.button('loading');
    var action = calli.getFormAction(form);
    if (action.indexOf('?create=') > 0) {
        return calli.resolve(form).then(function(form){
            var previously = form.getAttribute("resource");
            var ns = window.location.pathname.replace(/\/?$/, '/');
            var resource = ns + encodeURI(local).replace(/%25(\w\w)/g, '%$1').replace(/%20/g, '+');
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
            return calli.postTurtle(action, data);
        }).then(function(redirect){
            if (window.parent != window && parent.postMessage) {
                parent.postMessage('PUT src\n\n' + redirect, '*');
            }
            window.location.replace(redirect);
        }, function(error){
            btn.button('reset');
            return calli.error(error);
        });
    } else {
        window.console && window.console.log("This create page is deprecated, use a different URL");
        return calli.promptForNewResource(null, local).then(function(folder, local){
            if (!folder || !local) return undefined;
            var type = window.location.href.replace(/\?.*|\#.*/, '');
            var url = folder + '?create=' + encodeURIComponent(type);
            var resource = folder.replace(/\/?$/, '/') + local.replace(/%20/g, '+');
            form.setAttribute("resource", resource);
            return calli.postTurtle(url, calli.copyResourceData(form));
        }).then(function(redirect){
            if (redirect) {
                window.location.replace(redirect);
            }
        }, function(error){
            btn.button('reset');
            return calli.error(error);
        });
    }
};

calli.postTurtle = function(url, data) {
    return calli.createText(url, getTurtle(data), "text/turtle");
};

$(function($){
    $('form').submit(function(event, onlyHandlers) {
        if (this.getAttribute("enctype") != "text/turtle")
            return true;
        var form = $(this);
        if (!onlyHandlers && !event.isDefaultPrevented()) {
            event.preventDefault();
            event.stopImmediatePropagation();
            form.triggerHandler(event.type, true);
        } else if (onlyHandlers) {
            form.find(":input").change(); // IE may not have called onchange before onsubmit
            setTimeout(function(){
                var resource = form.attr('about') || form.attr('resource');
                if (resource && !event.isDefaultPrevented()) {
                    submitRDFForm(form[0], resource);
                }
            }, 0);
        }
    });
});

function submitRDFForm(form, uri) {
    var waiting = calli.wait();
    try {
        var data = calli.copyResourceData(form);
        var se = $.Event("calliSubmit");
        se.resource = data.resource;
        se.location = calli.getFormAction(form);
        se.payload = getTurtle(data);
        $(form).trigger(se);
        if (!se.isDefaultPrevented()) {
            var method = form.getAttribute('method') || form.method || "POST";
            postData(method, calli.getFormAction(form), se.payload, function(data, textStatus, xhr) {
                try {
                    var redirect = xhr.getResponseHeader("Location");
                    var contentType = xhr.getResponseHeader('Content-Type');
                    if (!redirect && contentType !== null && contentType.indexOf('text/uri-list') === 0) {
                        redirect = xhr.responseText;
                    }
                    if (!redirect) {
                        redirect = calli.getFormAction(form);
                        if (redirect.indexOf('?') > 0) {
                            redirect = redirect.substring(0, redirect.indexOf('?'));
                        }
                    }
                    var event = $.Event("calliRedirect");
                    event.cause = se;
                    event.resource = se.resource;
                    event.location = redirect;
                    $(form).trigger(event);
                    if (!event.isDefaultPrevented()) {
                        if (window.parent != window && parent.postMessage) {
                            parent.postMessage('PUT src\n\n' + event.location, '*');
                        }
                        window.location.replace(event.location);
                    }
                } catch(e) {
                    throw calli.error(e);
                }
            });
        }
    } catch(e) {
        throw calli.error(e);
    } finally {
        waiting.over();
    }
}

function getTurtle(data) {
    var serializer = new TurtleSerializer();
    serializer.setMappings(data.prefix);
    data.results.bindings.forEach(function(triple){
        serializer.addTriple(triple);
    });
    return serializer.toString();
}

function postData(method, url, data, callback) {
    var xhr = $.ajax({
        type: method,
        url: url,
        contentType: "text/turtle",
        data: data,
        dataType: "text", 
        xhrFields: calli.withCredentials,
        success: function(data, textStatus) {
            calli.lastModified(url, xhr.getResponseHeader('Last-Modified'));
            if (callback) {
                callback(data, textStatus, xhr);
            }
        },
        error: calli.error
    });
    return xhr;
}

})(jQuery);

