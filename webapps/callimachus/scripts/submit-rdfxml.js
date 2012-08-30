// submit-rdfxml.js
/*
   Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){

$('form[enctype="application/rdf+xml"]').submit(function(event) {
    var form = $(this);
    form.find("input").change(); // IE may not have called onchange before onsubmit
    var resource = form.attr('about') || form.attr('resource');
    if (!resource || resource.indexOf(':') < 0 && resource.indexOf('/') != 0 && resource.indexOf('?') != 0)
        return true; // resource attribute not set yet
    event.preventDefault();
    setTimeout(function(){submitRDFForm(form[0], form.attr('about') || form.attr('resource'));}, 0);
    return false;
});

function submitRDFForm(form, uri) {
    try {
        var data = getRDFXML(uri, form);
        postData(form, data, function(data, textStatus, xhr) {
            try {
                var redirect = xhr.getResponseHeader("Location");
                if (!redirect) {
                    redirect = calli.getPageUrl();
                    if (redirect.indexOf('?') > 0) {
                        redirect = redirect.substring(0, redirect.indexOf('?'));
                    }
                }
                var event = $.Event("calliRedirect");
                event.location = window.calli.viewpage(redirect);
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
    } catch(e) {
        throw calli.error(e);
    }
}

function getRDFXML(uri, form) {
    var 
        parser = new RDFaParser(),
        serializer = new RDFXMLSerializer(),
        formSubject = parser.parseURI(parser.getNodeBase(form)).resolve(uri),
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
        type = "application/rdf+xml";
    }
    var xhr = null;
    xhr = $.ajax({
        type: method,
        url: calli.getFormAction(form),
        contentType: type,
        data: data,
        beforeSend: withCredentials,
        success: function(data, textStatus) {
            callback(data, textStatus, xhr);
        }
    });
}

function withCredentials(req) {
    try {
        req.withCredentials = true;
    } catch (e) {}
}

});

