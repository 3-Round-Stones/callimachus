// submit-update.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Portions Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var calli = window.calli || (window.calli={});

calli.copyResourceData = (function(memo, element) {
    var form = calli.fixEvent(element).target;
    $(form).find('input,textarea,select').change();
    return readRDF(form);
}).bind(this, {});

calli.submitUpdate = function(comparedData, event) {
    event.preventDefault();
    var form = calli.fixEvent(event).target;
    var btn = $(form).find('button[type="submit"]');
    btn.button('loading');
    return calli.resolve(form).then(function(form){
        return calli.copyResourceData(form);
    }).then(function(insertData){
        insertData.results.bindings.push({
            s: {type:'uri', value: insertData.head.link[0]},
            p: {type:'uri', value: 'http://purl.org/dc/terms/modified'},
            o: {
                type:'literal',
                value: new Date().toISOString(),
                datatype: "http://www.w3.org/2001/XMLSchema#dateTime"
            }
        });
        return insertData;
    }).then(function(insertData){
        var action = calli.getFormAction(form);
        return calli.postUpdate(action, comparedData, insertData);
    }).then(function(redirect){
        window.location.replace(redirect);
    }, function(error){
        btn.button('reset');
        return calli.error(error);
    });
};

calli.postUpdate = function(url, deleteData, insertData) {
    return calli.resolve().then(function(){
        var diff = diffTriples(deleteData.results.bindings, insertData.results.bindings);
        var payload = asSparqlUpdate(insertData.prefix, diff.removed, diff.added);
        return calli.updateText(url, payload, "application/sparql-update");
    });
};

$(function(){
    $('form[enctype="application/sparql-update"]').each(function() {
        try {
            var form = $(this);
            form.find("input,textarea,select").change(); // give update-resource.js a chance to initialize
            var stored = readRDF(form[0]).results.bindings;
            form.bind('reset', function() {
                stored = readRDF(form[0]).results.bindings;
            });
            form.submit(function(event, onlyHandlers) {
                if (this.getAttribute("enctype") != "application/sparql-update")
                    return true;
                form.find("input,textarea,select").change(); // IE may not have called onchange before onsubmit
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
});

function submitRDFForm(form, resource, stored) {
    try {
        var parser = new RDFaParser();
        var uri = parser.parseURI(parser.getNodeBase(form)).resolve(resource);
        var data = parseRDF(parser, uri, form);
        var revised = data.results.bindings;
        var diff = diffTriples(stored, revised);
        var se = $.Event("calliSubmit");
        se.resource = uri;
        se.location = calli.getFormAction(form);
        se.payload = asSparqlUpdate(data.prefix, diff.removed, diff.added);
        $(form).trigger(se);
        if (!se.isDefaultPrevented()) {
            var method = form.getAttribute('method') || form.method || "POST";
            patchData(method, calli.getFormAction(form), se.payload, function(data, textStatus, xhr) {
                try {
                    var redirect = null;
                    var contentType = xhr.getResponseHeader('Content-Type');
                    if (contentType !== null && contentType.indexOf('text/uri-list') === 0) {
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
    }
    return false;
}

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
        base: fixedEncodeURI(parser.getNodeBase(form)),
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

function fixedEncodeURI (str) {
    return encodeURI(str).replace(/%5B/g, '[').replace(/%5D/g, ']').replace(/%25(\w\w)/g, '%$1');
}

/**
 * Makes sure blank subjects and objects get complemented with incoming and outgoing triples (transitive closure).
 */
function diffTriples(deleteTriples, insertTriples) {
    var result = {
        added: {},
        removed: {}
    };
    var oldTriples = deleteTriples.reduce(function(oldTriples, triple){
        oldTriples[JSON.stringify(triple)] = triple;
        return oldTriples;
    }, {});
    var newTriples = insertTriples.reduce(function(newTriples, triple){
        newTriples[JSON.stringify(triple)] = triple;
        return newTriples;
    }, {});
    // removed
    for (var ohash in oldTriples) {
        var old = oldTriples[ohash];
        if (!newTriples[ohash] || old.s.type == 'bnode' || old.o.type == 'bnode') {
            result.removed[ohash] = old;
        }
    }
    // added
    for (var nhash in newTriples) {
        var triple = newTriples[nhash];
        if (!oldTriples[nhash] || triple.s.type == 'bnode' || triple.o.type == 'bnode') {
            result.added[nhash] = triple;
        }
    }
    return result;
}

function asSparqlUpdate(namespaces, removed, added) {
    var writer = new UpdateWriter();
    writer.setMappings(namespaces);

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

function patchData(method, action, data, callback) {
    var xhr = $.ajax({ type: method, url: action, contentType: "application/sparql-update", data: data, dataType: "text", xhrFields: {withCredentials: true}, beforeSend: function(xhr){
        var modified = calli.lastModified(action);
        if (modified) {
            xhr.setRequestHeader("If-Unmodified-Since", modified);
        }
    }, success: function(data, textStatus) {
        calli.lastModified(action, xhr.getResponseHeader('Last-Modified'));
        if (callback) {
            callback(data, textStatus, xhr);
        }
    }, error: calli.error});
    return xhr;
}

function UpdateWriter() {
    this.prefixes = {};
    this.usedNamespaces = {};
    this.triples = {};
    this.buf = [];

    this.setMappings = function(namespaces) {
        for (var prefix in namespaces) {
            var namespace = namespaces[prefix];
            this.prefixes[namespace] = prefix;
        }
    }
    
    this.push = function(str) {
        return this.buf.push(str);
    };
    
    this.reset = function() {
        this.buf = [];
        return this;
    };
    
    this.toString = function() {
        var buf = [];
        for (var prefix in this.usedNamespaces) {
            buf.push('PREFIX '+ prefix + ':<' + this.usedNamespaces[prefix] + '>\n');
        }
        if (buf.length) {
            buf.push('\n');
        }
        return buf.concat(this.flush().buf).join('');
    };
    
    this.openDeleteWhere = function() {
        this.push('DELETE WHERE {\n');
    };
    
    this.closeDeleteWhere = function() {
        this.flush().push('};\n');
    };
    
    this.openInsert = function() {
        this.push('INSERT {\n');
    };
    
    this.closeInsert = function() {
        this.flush().push('}\n');
    };
    
    this.openWhere = function() {
        this.push('WHERE {\n');
    };
    
    this.closeWhere = function() {
        this.flush().push('};\n');
    };

    this.flush = function() {
        for (var subject in this.triples) {
            this.push('\t');
            this.push(subject);
            this.push('\n\t\t' + this.triples[subject].join(';\n\t\t') + '.\n');
        }
        this.triples = {};
        return this;
    }
    
    this.triple = function(triple) {
        var key = this.term(triple.s);
        if (!this.triples[key]) {
            this.triples[key] = [];
        }
        this.triples[key].push(this.term(triple.p) + ' ' + this.term(triple.o));
        return this;
    };
    
    /**
     * Serializes a triple for the DELETE/WHERE section, with bnodes replaced by vars.
     */ 
    this.pattern = function(triple) {
        var key;
        if (triple.s.type == 'bnode') {
            key = "?v" + triple.s.value;
        } else {
            key = this.term(triple.s);
        }
        if (!this.triples[key]) {
            this.triples[key] = [];
        }
        if (triple.o.type == 'bnode') {
            this.triples[key].push(this.term(triple.p) + ' ?v' + triple.o.value);
        } else {
            this.triples[key].push(this.term(triple.p) + ' ' + this.term(triple.o));
        }
        return this;
    };   
    
    this.term = function(term) {
        // bnode
        if (term.type == 'bnode') {
            return "_:" + term.value;
        }
        // uri
        else if (term.type == 'uri') {
            var namespace = term.value.replace(/[\w_\-\.\\%]+$/, '');
            var prefix = this.prefixes[namespace];
            if (term.value == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") {
                return 'a';
            } else if (prefix) {
                this.usedNamespaces[prefix] = namespace;
                return prefix + ':' + term.value.substring(namespace.length);
            } else {
                return '<' + term.value.replace(/\\/g, '\\\\').replace(/>/g, '\\>') + '>';
            }
        }
        // literal
        else {
            var buf = [];
            var s = term.value;
            if (term.datatype == "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral") {
                s = calli.decodeHtmlText(s, true);
            }
            if (s.indexOf('\n') >= 0 || s.indexOf('\t') >= 0 || s.indexOf('\r') >= 0) {
                buf.push('"""');
                buf.push(s.replace(/\\/g, "\\\\").replace(/"/g, '\\"'));
                buf.push('"""');
            } else {
                buf.push('"');
                buf.push(s.replace(/\\/g, "\\\\").replace(/"/g, '\\"'));
                buf.push('"');
            }
            // language
            if (term.datatype == "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString" && term["xml:lang"]) {
                buf.push('@');
                buf.push(term["xml:lang"].replace(/[^0-9a-zA-Z\-]/g, ''));
            }
            // datatype
            else if (term.datatype && term.datatype != "http://www.w3.org/2001/XMLSchema#string") {
                buf.push('^^');
                buf.push(this.term({type:'uri',value:term.datatype}));
            }
            return buf.join('');
        }
    };
}

})(jQuery);
