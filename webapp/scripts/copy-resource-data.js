// copy-resource-data.js
/*
   Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.copyResourceData = function(element) {
    var form = calli.fixEvent(element).target;
    $(form).find('input,textarea,select').change();
    return readRDF(form);
};

function readRDF(form) {
    var parser = new RDFaParser();
    var base = parser.getNodeBase(form);
    var resource = $(form).attr("about") || $(form).attr("resource") || '';
    var formSubject = resource ? parser.parseURI(base).resolve(resource) : base;
    return parseRDF(parser, formSubject, form);
}

function parseRDF(parser, formSubject, form) {
    var 
        formHash = formSubject + "#",
        bindings = [],
        usedBlanks = {},
        selfRefs = []
    ;
    parser.parse(form, function(s, p, o, dt, lang) {
        // keep subjects matching the form's subject and blank subjects if already introduced as objects
        if ((s == formSubject || s.indexOf(formHash) === 0 || usedBlanks[s]) && !isDecendent(this, selfRefs)) {
            // Resources linking to themselves don't need any triples under the reference.
            if (!dt && s == o) {
                selfRefs.push(this);
            }
            var binding = {
                s: bind(s),
                p: bind(p),
                o: bind(o, dt, lang)
            };
            bindings.push(binding);
            // log blank objects, they may be used as subjects in later triples
            if (!dt && o.indexOf('_:') === 0) {
                usedBlanks[o] = true;
            }
        }
    });
    return {
        base: parser.getNodeBase(form),
        prefix: parser.getMappings(),
        head: {
            link: [formSubject],
            vars: ['s', 'p', 'o']
        },
        results: {
            bindings: bindings
        }
    };
}

function isDecendent(child, parents) {
    var node = child;
    while (node) {
        if (parents.indexOf(node) >= 0)
            return true;
        node = node.parentNode;
    }
    return false;
}

function bind(o, dt, lang) {
    if (lang) {
        return {type:"literal", value: o, "xml:lang": lang};
    } else if (dt == "http://www.w3.org/2001/XMLSchema#string") {
        return {type:"literal", value: o};
    } else if (dt) {
        return {type:"literal", value: o, datatype: dt};
    } else if (o.match(/^_:/)) {
        return {type:"bnode", value: o.substring(2)};
    } else {
        return {type:"uri", value: o};
    }
}

})(jQuery);
