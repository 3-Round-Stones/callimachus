// submit-turtle.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

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
        var parser = new RDFaParser();
        var resource = parser.parseURI(parser.getNodeBase(form)).resolve(uri);
        var se = $.Event("calliSubmit");
        se.resource = resource;
        se.location = calli.getFormAction(form);
        se.payload = getTurtle(parser, resource, form);
        $(form).trigger(se);
        if (!se.isDefaultPrevented()) {
            postData(form, se.payload, function(data, textStatus, xhr) {
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

function getTurtle(parser, formSubject, form) {
    var 
        serializer = new TurtleSerializer(),
        usedBlanks = {},
        isBlankS,
        isFirstTriple = true
    ;
    parser.parse(form, function(s, p, o, dt, lang) {
        isBlankS = s.indexOf('_:') === 0;
        // keep subjects matching the form's subject and blank subjects if already introduced as objects
        if (s == formSubject || s.indexOf(formSubject + "#") === 0 || (isBlankS && usedBlanks[s])) {
            if (isFirstTriple) {
                serializer.setMappings(parser.getMappings());// import prefixes encountered so far
                isFirstTriple = false;
            }
            serializer.addTriple(s, p, o, dt, lang);
            // log blank objects, they may be used as subjects in later triples
            if (!dt && o.indexOf('_:') === 0) {
                usedBlanks[o] = true;
            }
        }
    });
    return serializer.toString();
}

function postData(form, data, callback) {
    var method = form.getAttribute('method');
    if (!method) {
        method = form.method;
    }
    if (!method) {
        method = "POST";
    }
    var type = form.getAttribute("enctype");
    if (!type) {
        type = "text/turtle";
    }
    var xhr = null;
    xhr = $.ajax({
        type: method,
        url: calli.getFormAction(form),
        contentType: type,
        data: data,
        dataType: "text", 
        xhrFields: calli.withCredentials,
        success: function(data, textStatus) {
                callback(data, textStatus, xhr);
            }
    });
}

})(jQuery);

