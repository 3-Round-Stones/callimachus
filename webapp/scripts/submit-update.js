// submit-update.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Portions Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){

$('form[enctype="application/sparql-update"]').each(function() {
    try {
        var form = $(this);
        form.find(":input").change(); // give update-resource.js a chance to initialize
        var stored = readRDF(form[0]);
        form.bind('reset', function() {
            stored = readRDF(form[0]);
        });
        form.submit(function(event, onlyHandlers) {
            if (this.getAttribute("enctype") != "application/sparql-update")
                return true;
            form.find(":input").change(); // IE may not have called onchange before onsubmit
            if (!onlyHandlers && !event.isDefaultPrevented()) {
                event.preventDefault();
                event.stopImmediatePropagation();
                form.triggerHandler(event.type, true);
            } else if (onlyHandlers) {
                setTimeout(function(){
                    var resource = form.attr('about') || form.attr('resource');
                    if (resource && !event.isDefaultPrevented()) {
                        submitRDFForm(form[0], resource, stored);
                    }
                }, 0);
            }
        });
    } catch (e) {
        throw calli.error(e);
    }
});

function submitRDFForm(form, resource, stored) {
    var waiting = calli.wait();
    try {
        var parser = new RDFaParser();
        var uri = parser.parseURI(parser.getNodeBase(form)).resolve(resource);
        var revised = parseRDF(parser, uri, form);
        var diff = diffTriples(stored, revised);
        var removed = diff.removed;
        var added = diff.added;
        for (var rhash in removed) {
            addBoundedDescription(removed[rhash], stored, removed, added);
        }
        for (var ahash in added) {
            addBoundedDescription(added[ahash], revised, added, removed);
        }
        var se = $.Event("calliSubmit");
        se.resource = uri;
        se.location = calli.getFormAction(form);
        se.payload = asSparqlUpdate(removed, added);
        $(form).trigger(se);
        if (!se.isDefaultPrevented()) {
            patchData(form, se.payload, function(data, textStatus, xhr) {
                try {
                    var redirect = null;
                    var contentType = xhr.getResponseHeader('Content-Type');
                    if (contentType != null && contentType.indexOf('text/uri-list') == 0) {
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
    return false;
}

function readRDF(form) {
    var parser = new RDFaParser();
    var base = parser.getNodeBase(form);
    var resource = $(form).attr("about") || $(form).attr("resource");
    var formSubject = resource ? parser.parseURI(base).resolve(resource) : base;
    return parseRDF(parser, formSubject, form);
}

function parseRDF(parser, formSubject, form) {
    var 
        writer = new UpdateWriter(),
        formHash = formSubject + "#",
        triples = {},
        usedBlanks = {},
        selfRefs = {},
        hash
    ;
    parser.parse(form, function(s, p, o, dt, lang) {
        // keep subjects matching the form's subject and blank subjects if already introduced as objects
        if ((s == formSubject || s.indexOf(formHash) === 0 || usedBlanks[s]) && !isDecendent(this, selfRefs)) {
            // Resources linking to themselves don't need any triples under the reference.
            if (!dt && s == o) {
                selfRefs[this] = true;
            }
            hash = writer.hash(s, p, o, dt, lang);
            triples[hash] = {subject: s, predicate: p, object: o, datatype: dt, language: lang};
            // log blank objects, they may be used as subjects in later triples
            if (!dt && o.indexOf('_:') === 0) {
                usedBlanks[o] = true;
            }
        }
    });
    return triples;
}

function isDecendent(child, parents) {
    var node = child;
    while (node) {
        if (parents[node])
            return true;
        node = node.parentNode;
    }
    return false;
}

function diffTriples(oldTriples, newTriples) {
    var 
        added = {},
        removed = {},
        hash
    ;
    // removed
    for (hash in oldTriples) {
        if (!newTriples[hash]) {
            removed[hash] = oldTriples[hash];
        }
    }
    // added
    for (hash in newTriples) {
        if (!oldTriples[hash]) {
            added[hash] = newTriples[hash];
        }
    }
    return {added: added, removed: removed};
}

/**
 * Makes sure blank subjects and objects get complemented with incoming and outgoing triples (transitive closure).
 */
function addBoundedDescription(triple, store, dest, copy) {
    var hash;
    if (triple.subject.match(/^_:/)) {
        for (hash in store) {
            if (store[hash].object == triple.subject && !store[hash].datatype && !dest[hash]) {
                copy[hash] = dest[hash] = store[hash];
                addBoundedDescription(store[hash], store, dest, copy);
            }
        }
    }
    if (triple.object.match(/^_:/) && !triple.datatype) {
        for (hash in store) {
            if (store[hash].subject == triple.object && !dest[hash]) {
                copy[hash] = dest[hash] = store[hash];
                addBoundedDescription(store[hash], store, dest, copy);
            }
        }
    }
}

function asSparqlUpdate(removed, added) {
    var writer = new UpdateWriter();

    if (removed && !$.isEmptyObject(removed)) {
        writer.openDeleteWhere();
        for (var triple in removed) {
            writer.pattern(removed[triple]);
        }
        writer.closeDeleteWhere();
    }

    if (added && !$.isEmptyObject(added)) {
        writer.openInsert();
        for (var triple in added) {
            writer.triple(added[triple]);
        }
        writer.closeInsert();
        writer.openWhere();
        writer.closeWhere();
    }

    return writer.toString() || 'INSERT {} WHERE {}';
}

function patchData(form, data, callback) {
    var method = form.getAttribute('method');
    if (!method) {
        method = form.method;
    }
    if (!method) {
        method = "POST";
    }
    var type = form.getAttribute("enctype");
    if (!type) {
        type = "application/sparql-update";
    }
    var action = calli.getFormAction(form);
    var xhr = $.ajax({ type: method, url: action, contentType: type, data: data, dataType: "text", xhrFields: calli.withCredentials, beforeSend: function(xhr){
        var modified = calli.lastModified(action);
        if (modified) {
            xhr.setRequestHeader("If-Unmodified-Since", modified);
        }
    }, success: function(data, textStatus) {
        calli.lastModified(action, new Date().toUTCString());
        callback(data, textStatus, xhr);
    }});
}

function UpdateWriter() {
    this.buf = [];
    
    this.push = function(str) {
        return this.buf.push(str);
    };
    
    this.reset = function() {
        this.buf = [];
        return this;
    };
    
    this.toString = function() {
        return this.buf.join('');
    };
    
    this.openDeleteWhere = function() {
        this.push('DELETE WHERE {\n');
    };
    
    this.closeDeleteWhere = function() {
        this.push('};\n');
    };
    
    this.openInsert = function() {
        this.push('INSERT {\n');
    };
    
    this.closeInsert = function() {
        this.push('}\n');
    };
    
    this.openWhere = function() {
        this.push('WHERE {\n');
    };
    
    this.closeWhere = function() {
        this.push('};\n');
    };
    
    this.triple = function(triple) {
        this.push('\t');
        this.term(triple.subject, null, null);
        this.push(' ');
        this.term(triple.predicate, null, null);
        this.push(' ');
        this.term(triple.object, triple.datatype, triple.language);
        this.push(' .\n');
        return this;
    };
    
    this.hash = function(subject, predicate, object, datatype, language) {
        var tempWriter = new UpdateWriter();
        return tempWriter.triple({
            subject:subject, predicate:predicate, object:object,
            datatype:datatype, language:language
        }).toString();
    };
    
    /**
     * Serializes a triple for the DELETE/WHERE section, with bnodes replaced by vars.
     */ 
    this.pattern = function(triple) {
        this.push('\t');
        if (triple.subject.match(/^_:/)) {
            this.push("?var" + triple.subject.substring(2));
        } else {
            this.term(triple.subject, null, null);
        }
        this.push(' ');
        this.term(triple.predicate, null, null);
        this.push(' ');
        if (triple.object.match(/^_:/) && !triple.datatype) {
            this.push("?var" + triple.object.substring(2));
        } else {
            this.term(triple.object, triple.datatype, triple.language);
        }
        this.push(' .\n');
        return this;
    };   
    
    this.term = function(term, datatype, language) {
        // bnode
        if (!datatype && term.match(/^_:/)) {
            this.push("_:bn" + term.substring(2));
        }
        // uri
        else if (!datatype) {
            this.push('<');
            this.push(term.replace(/\\/g, '\\\\').replace(/>/g, '\\>'));
            this.push('>');
        }
        // literal
        else {
            var s = term;
            if (datatype == "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral") {
                s = calli.decodeHtmlText(s, true);
            }
            this.push('"');
            s = s
                .replace(/\\/g, "\\\\")
                .replace(/\t/g, "\\t")
                .replace(/\n/g, "\\n")
                .replace(/\r/g, "\\r")
                .replace(/"/g, '\\"')
            ;
            this.push(s);
            this.push('"');
            // language
            if (datatype == "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString" && language) {
                this.push('@');
                this.push(language.replace(/[^0-9a-zA-Z\-]/g, ''));
            }
            // datatype
            else if (datatype != "http://www.w3.org/2001/XMLSchema#string") {
                this.push('^^');
                this.push('<');
                this.push(datatype.replace(/\\/g, '\\\\').replace(/>/g, '\\>'));
                this.push('>');
            }
        }
    };
}

});
