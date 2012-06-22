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
        var stored = readRDF(form);
        form.bind('reset', function() {
            stored = readRDF(form);
        });
        form.submit(function(event) {
            form.find("input").change(); // IE may not have called onchange before onsubmit
            var resource = form.attr('about') || form.attr('resource');
            if (!resource || resource.indexOf(':') < 0 && resource.indexOf('/') != 0 && resource.indexOf('?') != 0)
                return true; // resource attribute not set
            event.preventDefault();
            setTimeout(function(){submitRDFForm(form, stored);}, 0);
            return false;
        });
    } catch (e) {
        throw calli.error(e);
    }
});

function submitRDFForm(form, stored) {
    var se = $.Event("calliSubmit");
    form.trigger(se);
    if (!se.isDefaultPrevented()) {
        try {
            var revised = readRDF(form);
            var removed = stored.except(revised);
            var added = revised.except(stored);
            removed.triples().each(function(){
                addBoundedDescription(this, stored, removed, added);
            });
            added.triples().each(function(){
                addBoundedDescription(this, revised, added, removed);
            });
            var writer = new UpdateWriter();
            var namespaces = form.xmlns();
            for (var prefix in namespaces) {
                writer.prefix(prefix, namespaces[prefix].toString());
            }
            writer.openDelete();
            removed.triples().each(function() {
                writer.triple(this.subject, this.property, this.object);
            });
            writer.closeDelete();
            writer.openInsert();
            added.triples().each(function() {
                writer.triple(this.subject, this.property, this.object);
            });
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
                    form.trigger(event);
                    if (!event.isDefaultPrevented()) {
                        if (window.parent != window && parent.postMessage) {
                            parent.postMessage('PUT src\n\n' + event.location, '*');
                        }
                        location.replace(event.location);
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
    var subj = $.uri.base()
    var resource = $(form).attr("about") || $(form).attr("resource");
    if (resource) {
        subj = subj.resolve(resource)
    }
    var store = form.rdf().databank
    store.triples().each(function(){
        if (this.subject.type == 'uri' && this.subject.value.toString() != subj.toString() && this.subject.value.toString().indexOf(subj.toString() + '#') != 0) {
            store.remove(this)
        } else if (this.subject.type == "bnode") {
            var orphan = true
            $.rdf({databank: store}).where("?s ?p " + this.subject).each(function (i, bindings, triples) {
                orphan = false
            })
            if (orphan) {
                store.remove(this)
            }
        }
    })
    return store
}

function addBoundedDescription(triple, store, dest, copy) {
    if (triple.subject.type == "bnode") {
        var bnode = triple.subject
        $.rdf({databank: store}).where("?s ?p " + bnode).each(function (i, bindings, triples) {
            if (addTriple(triples[0], dest)) {
                copy.add(triples[0])
                addBoundedDescription(triples[0], store, dest, copy)
            }
        })
    }
    if (triple.object.type == "bnode") {
        var bnode = triple.object
        $.rdf({databank: store}).where(bnode + ' ?p ?o').each(function (i, bindings, triples) {
            if (addTriple(triples[0], dest)) {
                copy.add(triples[0])
                addBoundedDescription(triples[0], store, dest, copy)
            }
        })
    }
}

function addTriple(triple, store) {
    var size = store.size()
    store.add(triple)
    return store.size() > size
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
    xhr = $.ajax({ type: method, url: calli.getFormAction(form), contentType: type, data: data, beforeSend: function(xhr){
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
}

UpdateWriter.prototype = {
    push: function(str) {
        return this.buf.push(str);
    },
    toString: function() {
        return this.buf.join('');
    },
    prefix: function(prefix, uri) {
        this.push('PREFIX ');
        this.push(prefix);
        this.push(':');
        this.push('<');
        this.push(uri);
        this.push('>');
        this.push('\n');
    },
    openDelete: function() {
        this.push('DELETE {\n');
    },
    closeDelete: function() {
        this.push('}\n');
    },
    openInsert: function() {
        this.push('INSERT {\n');
    },
    closeInsert: function() {
        this.push('}\n');
    },
    openWhere: function() {
        this.push('WHERE {\n');
    },
    closeWhere: function() {
        this.push('}\n');
    },
    triple: function(subject, predicate, object) {
        this.push('\t');
        this.term(subject);
        this.push(' ');
        this.term(predicate);
        this.push(' ');
        this.term(object);
        this.push(' .\n');
    },
    term: function(term) {
        if (term.type == 'uri') {
            this.push('<');
            this.push(term.value.toString().replace(/\\/g, '\\\\').replace(/>/g, '\\>'));
            this.push('>');
        } else if (term.type == 'bnode') {
            this.push(term.value.toString());
        } else if (term.type == 'literal') {
            var s = term.value.toString();
            if (term.datatype && term.datatype.toString() == "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral") {
                s = removeHtmlEntities(s);
            }
            this.push('"');
            s = s.replace(/\\/g, "\\\\");
            s = s.replace(/\t/g, "\\t");
            s = s.replace(/\n/g, "\\n");
            s = s.replace(/\r/g, "\\r");
            s = s.replace(/"/g, '\\"');
            this.push(s);
            this.push('"');
            if (term.datatype !== undefined) {
                this.push('^^');
                this.push('<');
                this.push(term.datatype.toString().replace(/\\/g, '\\\\').replace(/>/g, '\\>'));
                this.push('>');
            }
            if (term.lang !== undefined) {
                this.push('@');
                this.push(term.lang.toString().replace(/[^0-9a-zA-Z\-]/g, ''));
            }
        } else if (!term.type) {
            throw "Unknown term: " + term;
        } else {
            throw "Unknown term type: " + term.type;
        }
    }
};

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

