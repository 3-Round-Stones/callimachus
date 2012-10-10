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
        var stored = readRDF(form[0]);
        form.bind('reset', function() {
            stored = readRDF(form[0]);
        });
        form.submit(function(event) {
            form.find("input").change(); // IE may not have called onchange before onsubmit
            var resource = form.attr('about') || form.attr('resource');
            if (!resource || resource.indexOf(':') < 0 && resource.indexOf('/') != 0 && resource.indexOf('?') != 0)
                return true; // resource attribute not set
            event.preventDefault();
            setTimeout(function(){submitRDFForm(form[0], stored);}, 0);
            return false;
        });
    } catch (e) {
        throw calli.error(e);
    }
});

function submitRDFForm(form, stored) {
    var se = $.Event("calliSubmit");
    $(form).trigger(se);
    if (!se.isDefaultPrevented()) {
        try {
            var revised = readRDF(form);
            var diff = diffTriples(stored, revised);
            var removed = cleanSelfReferences(diff.removed);
            var added = cleanSelfReferences(diff.added);
            for (hash in removed) {
                addBoundedDescription(removed[hash], stored, removed, added);
            }
            for (hash in added) {
                addBoundedDescription(added[hash], revised, added, removed);
            }
            
            var writer = new UpdateWriter();
            writer.openDelete();
            for (triple in removed) {
                writer.push(triple);
            }
            writer.closeDelete();
            writer.openInsert();
            for (triple in added) {
                writer.push(triple);
            }
            writer.closeInsert();
            writer.openWhere();
            writer.closeWhere();
            var data = writer.toString();
            patchData(form[0], data, function(data, textStatus, xhr) {
                try {
                    var redirect = xhr.getResponseHeader("Location");
                    if (!redirect) {
                        redirect = calli.getPageUrl();
                        if (redirect.indexOf('?') > 0) {
                            redirect = redirect.substring(0, redirect.indexOf('?'));
                        }
                    }
                    redirect = redirect + "?view";
                    var event = $.Event("calliRedirect");
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
            })
        } catch(e) {
            throw calli.error(e);
        }
    }
    return false;
}

function readRDF(form) {
    var 
        parser = new RDFaParser(),
        writer = new UpdateWriter(),
        resource = $(form).attr("about") || $(form).attr("resource"),
        base = parser.getNodeBase(form),
        formSubject = resource ? parser.parseURI(base).resolve(resource) : base,
        triples = {},
        usedBlanks = {},
        isBlankS,
        hash
    ;
    parser.parse(form, function(s, p, o, dt, lang) {
        isBlankS = s.indexOf('_:') === 0;
        // keep subjects matching the form's subject and blank subjects if already introduced as objects
        if (s == formSubject || s.indexOf(formSubject + "#") === 0 || (isBlankS && usedBlanks[s])) {
            hash = writer.reset().triple(s, p, o, dt, lang).toString();
            triples[hash] = {subject: s, predicate: p, object: o, datatype: dt, language: lang};
            // log blank objects, they may be used as subjects in later triples
            if (!dt && o.indexOf('_:') === 0) {
                usedBlanks[o] = true;
            }
        }
    });
    return triples;
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
 * Resources linking to themselves don't need any triples beyond the reference. 
 */ 
function cleanSelfReferences(triples) {
    var 
        cleaned = {},
        selfRefs = {},
        hash
    ;
    // 1st iteration: keep (just) the self-references and flag the resource
    for (hash in triples) {
        if (triples[hash].subject == triples[hash].object) {
            selfRefs[triples[hash].subject] = true;
            cleaned[hash] = triples[hash];
        }
    }
    // 2nd iteration: add all triples not related to self-referring resources
    for (hash in triples) {
        if (triples[hash].subject == triples[hash].object) continue; // got them already
        if (selfRefs[triples[hash].subject]) continue // skip these
        cleaned[hash] = triples[hash];
    }
    return cleaned;
}

/**
 * Makes sure blank subjects and objects get complemented with incoming and outgoing triples (transitive closure).
 */
function addBoundedDescription(triple, store, dest, copy) {
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
    var xhr = null;
    xhr = $.ajax({ type: method, url: calli.getFormAction(form), contentType: type, data: data, dataType: "text", beforeSend: function(xhr){
        var lastmod = getLastModified();
        if (lastmod) {
            xhr.setRequestHeader("If-Unmodified-Since", lastmod);
        }
        withCredentials(xhr);
    }, success: function(data, textStatus) {
        callback(data, textStatus, xhr);
    }});
    function withCredentials(req) {
        try {
            req.withCredentials = true;
        } catch (e) {}
    }
}

function getLastModified() {
    try {
        var committedOn = $('#resource-lastmod').find('[property=audit:committedOn]').attr('content');
        return new Date(committedOn).toGMTString();
    } catch (e) {
        return null;
    }
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
    
    this.openDelete = function() {
        this.push('DELETE {\n');
    };
    
    this.closeDelete = function() {
        this.push('}\n');
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
        this.push('}\n');
    };
    
    this.triple = function(subject, predicate, object, datatype, language) {
        this.push('\t');
        this.term(subject, null, null);
        this.push(' ');
        this.term(predicate, null, null);
        this.push(' ');
        this.term(object, datatype, language);
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
                s = removeHtmlEntities(s);
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

function removeHtmlEntities(html) {
    html = html.replace(/&nbsp;/g,'&#160;');
    html = html.replace(/&iexcl;/g,'¡');
    html = html.replace(/&cent;/g,'¢');
    html = html.replace(/&pound;/g,'£');
    html = html.replace(/&curren;/g,'¤');
    html = html.replace(/&yen;/g,'¥');
    html = html.replace(/&brvbar;/g,'¦');
    html = html.replace(/&sect;/g,'§');
    html = html.replace(/&uml;/g,'¨');
    html = html.replace(/&copy;/g,'©');
    html = html.replace(/&ordf;/g,'ª');
    html = html.replace(/&laquo;/g,'«');
    html = html.replace(/&not;/g,'¬');
    html = html.replace(/&shy;/g,'&#173;');
    html = html.replace(/&reg;/g,'®');
    html = html.replace(/&macr;/g,'¯');
    html = html.replace(/&deg;/g,'°');
    html = html.replace(/&plusmn;/g,'±');
    html = html.replace(/&sup2;/g,'²');
    html = html.replace(/&sup3;/g,'³');
    html = html.replace(/&acute;/g,'´');
    html = html.replace(/&micro;/g,'µ');
    html = html.replace(/&para;/g,'¶');
    html = html.replace(/&middot;/g,'·');
    html = html.replace(/&cedil;/g,'¸');
    html = html.replace(/&sup1;/g,'¹');
    html = html.replace(/&ordm;/g,'º');
    html = html.replace(/&raquo;/g,'»');
    html = html.replace(/&frac14;/g,'¼');
    html = html.replace(/&frac12;/g,'½');
    html = html.replace(/&frac34;/g,'¾');
    html = html.replace(/&iquest;/g,'¿');
    html = html.replace(/&Agrave;/g,'À');
    html = html.replace(/&Aacute;/g,'Á');
    html = html.replace(/&Acirc;/g,'Â');
    html = html.replace(/&Atilde;/g,'Ã');
    html = html.replace(/&Auml;/g,'Ä');
    html = html.replace(/&Aring;/g,'Å');
    html = html.replace(/&AElig;/g,'Æ');
    html = html.replace(/&Ccedil;/g,'Ç');
    html = html.replace(/&Egrave;/g,'È');
    html = html.replace(/&Eacute;/g,'É');
    html = html.replace(/&Ecirc;/g,'Ê');
    html = html.replace(/&Euml;/g,'Ë');
    html = html.replace(/&Igrave;/g,'Ì');
    html = html.replace(/&Iacute;/g,'Í');
    html = html.replace(/&Icirc;/g,'Î');
    html = html.replace(/&Iuml;/g,'Ï');
    html = html.replace(/&ETH;/g,'Ð');
    html = html.replace(/&Ntilde;/g,'Ñ');
    html = html.replace(/&Ograve;/g,'Ò');
    html = html.replace(/&Oacute;/g,'Ó');
    html = html.replace(/&Ocirc;/g,'Ô');
    html = html.replace(/&Otilde;/g,'Õ');
    html = html.replace(/&Ouml;/g,'Ö');
    html = html.replace(/&times;/g,'×');
    html = html.replace(/&Oslash;/g,'Ø');
    html = html.replace(/&Ugrave;/g,'Ù');
    html = html.replace(/&Uacute;/g,'Ú');
    html = html.replace(/&Ucirc;/g,'Û');
    html = html.replace(/&Uuml;/g,'Ü');
    html = html.replace(/&Yacute;/g,'Ý');
    html = html.replace(/&THORN;/g,'Þ');
    html = html.replace(/&szlig;/g,'ß');
    html = html.replace(/&agrave;/g,'à');
    html = html.replace(/&aacute;/g,'á');
    html = html.replace(/&acirc;/g,'â');
    html = html.replace(/&atilde;/g,'ã');
    html = html.replace(/&auml;/g,'ä');
    html = html.replace(/&aring;/g,'å');
    html = html.replace(/&aelig;/g,'æ');
    html = html.replace(/&ccedil;/g,'ç');
    html = html.replace(/&egrave;/g,'è');
    html = html.replace(/&eacute;/g,'é');
    html = html.replace(/&ecirc;/g,'ê');
    html = html.replace(/&euml;/g,'ë');
    html = html.replace(/&igrave;/g,'ì');
    html = html.replace(/&iacute;/g,'í');
    html = html.replace(/&icirc;/g,'î');
    html = html.replace(/&iuml;/g,'ï');
    html = html.replace(/&eth;/g,'ð');
    html = html.replace(/&ntilde;/g,'ñ');
    html = html.replace(/&ograve;/g,'ò');
    html = html.replace(/&oacute;/g,'ó');
    html = html.replace(/&ocirc;/g,'ô');
    html = html.replace(/&otilde;/g,'õ');
    html = html.replace(/&ouml;/g,'ö');
    html = html.replace(/&divide;/g,'÷');
    html = html.replace(/&oslash;/g,'ø');
    html = html.replace(/&ugrave;/g,'ù');
    html = html.replace(/&uacute;/g,'ú');
    html = html.replace(/&ucirc;/g,'û');
    html = html.replace(/&uuml;/g,'ü');
    html = html.replace(/&yacute;/g,'ý');
    html = html.replace(/&thorn;/g,'þ');
    html = html.replace(/&yuml;/g,'ÿ');
    html = html.replace(/&OElig;/g,'Œ');
    html = html.replace(/&oelig;/g,'œ');
    html = html.replace(/&Scaron;/g,'Š');
    html = html.replace(/&scaron;/g,'š');
    html = html.replace(/&Yuml;/g,'Ÿ');
    html = html.replace(/&fnof;/g,'ƒ');
    html = html.replace(/&circ;/g,'ˆ');
    html = html.replace(/&tilde;/g,'˜');
    html = html.replace(/&Alpha;/g,'Α');
    html = html.replace(/&Beta;/g,'Β');
    html = html.replace(/&Gamma;/g,'Γ');
    html = html.replace(/&Delta;/g,'Δ');
    html = html.replace(/&Epsilon;/g,'Ε');
    html = html.replace(/&Zeta;/g,'Ζ');
    html = html.replace(/&Eta;/g,'Η');
    html = html.replace(/&Theta;/g,'Θ');
    html = html.replace(/&Iota;/g,'Ι');
    html = html.replace(/&Kappa;/g,'Κ');
    html = html.replace(/&Lambda;/g,'Λ');
    html = html.replace(/&Mu;/g,'Μ');
    html = html.replace(/&Nu;/g,'Ν');
    html = html.replace(/&Xi;/g,'Ξ');
    html = html.replace(/&Omicron;/g,'Ο');
    html = html.replace(/&Pi;/g,'Π');
    html = html.replace(/&Rho;/g,'Ρ');
    html = html.replace(/&Sigma;/g,'Σ');
    html = html.replace(/&Tau;/g,'Τ');
    html = html.replace(/&Upsilon;/g,'Υ');
    html = html.replace(/&Phi;/g,'Φ');
    html = html.replace(/&Chi;/g,'Χ');
    html = html.replace(/&Psi;/g,'Ψ');
    html = html.replace(/&Omega;/g,'Ω');
    html = html.replace(/&alpha;/g,'α');
    html = html.replace(/&beta;/g,'β');
    html = html.replace(/&gamma;/g,'γ');
    html = html.replace(/&delta;/g,'δ');
    html = html.replace(/&epsilon;/g,'ε');
    html = html.replace(/&zeta;/g,'ζ');
    html = html.replace(/&eta;/g,'η');
    html = html.replace(/&theta;/g,'θ');
    html = html.replace(/&iota;/g,'ι');
    html = html.replace(/&kappa;/g,'κ');
    html = html.replace(/&lambda;/g,'λ');
    html = html.replace(/&mu;/g,'μ');
    html = html.replace(/&nu;/g,'ν');
    html = html.replace(/&xi;/g,'ξ');
    html = html.replace(/&omicron;/g,'ο');
    html = html.replace(/&pi;/g,'π');
    html = html.replace(/&rho;/g,'ρ');
    html = html.replace(/&sigmaf;/g,'ς');
    html = html.replace(/&sigma;/g,'σ');
    html = html.replace(/&tau;/g,'τ');
    html = html.replace(/&upsilon;/g,'υ');
    html = html.replace(/&phi;/g,'φ');
    html = html.replace(/&chi;/g,'χ');
    html = html.replace(/&psi;/g,'ψ');
    html = html.replace(/&omega;/g,'ω');
    html = html.replace(/&thetasym;/g,'ϑ');
    html = html.replace(/&upsih;/g,'ϒ');
    html = html.replace(/&piv;/g,'ϖ');
    html = html.replace(/&ensp;/g,'&#8194;');
    html = html.replace(/&emsp;/g,'&#8195;');
    html = html.replace(/&thinsp;/g,'&#8201;');
    html = html.replace(/&zwnj;/g,'&#8204;');
    html = html.replace(/&zwj;/g,'&#8205;');
    html = html.replace(/&lrm;/g,'&#8206;');
    html = html.replace(/&rlm;/g,'&#8207;');
    html = html.replace(/&ndash;/g,'–');
    html = html.replace(/&mdash;/g,'—');
    html = html.replace(/&lsquo;/g,'‘');
    html = html.replace(/&rsquo;/g,'’');
    html = html.replace(/&sbquo;/g,'‚');
    html = html.replace(/&ldquo;/g,'“');
    html = html.replace(/&rdquo;/g,'”');
    html = html.replace(/&bdquo;/g,'„');
    html = html.replace(/&dagger;/g,'†');
    html = html.replace(/&Dagger;/g,'‡');
    html = html.replace(/&bull;/g,'•');
    html = html.replace(/&hellip;/g,'…');
    html = html.replace(/&permil;/g,'‰');
    html = html.replace(/&prime;/g,'′');
    html = html.replace(/&Prime;/g,'″');
    html = html.replace(/&lsaquo;/g,'‹');
    html = html.replace(/&rsaquo;/g,'›');
    html = html.replace(/&oline;/g,'‾');
    html = html.replace(/&frasl;/g,'⁄');
    html = html.replace(/&euro;/g,'€');
    html = html.replace(/&image;/g,'ℑ');
    html = html.replace(/&weierp;/g,'℘');
    html = html.replace(/&real;/g,'ℜ');
    html = html.replace(/&trade;/g,'™');
    html = html.replace(/&alefsym;/g,'ℵ');
    html = html.replace(/&larr;/g,'←');
    html = html.replace(/&uarr;/g,'↑');
    html = html.replace(/&rarr;/g,'→');
    html = html.replace(/&darr;/g,'↓');
    html = html.replace(/&harr;/g,'↔');
    html = html.replace(/&crarr;/g,'↵');
    html = html.replace(/&lArr;/g,'⇐');
    html = html.replace(/&uArr;/g,'⇑');
    html = html.replace(/&rArr;/g,'⇒');
    html = html.replace(/&dArr;/g,'⇓');
    html = html.replace(/&hArr;/g,'⇔');
    html = html.replace(/&forall;/g,'∀');
    html = html.replace(/&part;/g,'∂');
    html = html.replace(/&exist;/g,'∃');
    html = html.replace(/&empty;/g,'∅');
    html = html.replace(/&nabla;/g,'∇');
    html = html.replace(/&isin;/g,'∈');
    html = html.replace(/&notin;/g,'∉');
    html = html.replace(/&ni;/g,'∋');
    html = html.replace(/&prod;/g,'∏');
    html = html.replace(/&sum;/g,'∑');
    html = html.replace(/&minus;/g,'−');
    html = html.replace(/&lowast;/g,'∗');
    html = html.replace(/&radic;/g,'√');
    html = html.replace(/&prop;/g,'∝');
    html = html.replace(/&infin;/g,'∞');
    html = html.replace(/&ang;/g,'∠');
    html = html.replace(/&and;/g,'∧');
    html = html.replace(/&or;/g,'∨');
    html = html.replace(/&cap;/g,'∩');
    html = html.replace(/&cup;/g,'∪');
    html = html.replace(/&int;/g,'∫');
    html = html.replace(/&there4;/g,'∴');
    html = html.replace(/&sim;/g,'∼');
    html = html.replace(/&cong;/g,'≅');
    html = html.replace(/&asymp;/g,'≈');
    html = html.replace(/&ne;/g,'≠');
    html = html.replace(/&equiv;/g,'≡');
    html = html.replace(/&le;/g,'≤');
    html = html.replace(/&ge;/g,'≥');
    html = html.replace(/&sub;/g,'⊂');
    html = html.replace(/&sup;/g,'⊃');
    html = html.replace(/&nsub;/g,'⊄');
    html = html.replace(/&sube;/g,'⊆');
    html = html.replace(/&supe;/g,'⊇');
    html = html.replace(/&oplus;/g,'⊕');
    html = html.replace(/&otimes;/g,'⊗');
    html = html.replace(/&perp;/g,'⊥');
    html = html.replace(/&sdot;/g,'⋅');
    html = html.replace(/&lceil;/g,'⌈');
    html = html.replace(/&rceil;/g,'⌉');
    html = html.replace(/&lfloor;/g,'⌊');
    html = html.replace(/&rfloor;/g,'⌋');
    html = html.replace(/&lang;/g,'〈');
    html = html.replace(/&rang;/g,'〉');
    html = html.replace(/&loz;/g,'◊');
    html = html.replace(/&spades;/g,'♠');
    html = html.replace(/&clubs;/g,'♣');
    html = html.replace(/&hearts;/g,'♥');
    html = html.replace(/&diams;/g,'♦');
    return html;
}

});

