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
			var about = form.attr('about');
			if (!about || about.indexOf(':') < 0 && about.indexOf('/') != 0 && about.indexOf('?') != 0)
				return true; // about attribute not set
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
						redirect = getPageLocationURL();
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
	if ($(form).attr("about")) {
		subj = subj.resolve($(form).attr("about"))
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
	xhr = $.ajax({ type: method, url: form.action, contentType: type, data: data, beforeSend: function(xhr){
		var lastmod = getLastModified();
		if (lastmod) {
			xhr.setRequestHeader("If-Unmodified-Since", lastmod);
		}
	}, success: function(data, textStatus) {
		callback(data, textStatus, xhr);
	}})
}

function getLastModified() {
	try {
		var committedOn = $('#resource-lastmod').find('[property=audit:committedOn]').attr('content');
		return new Date(committedOn).toGMTString();
	} catch (e) {
		return null;
	}
}

function getPageLocationURL() {
	// window.location.href needlessly decodes URI-encoded characters in the URI path
	// https://bugs.webkit.org/show_bug.cgi?id=30225
	var path = location.pathname;
	if (path.match(/#/))
		return location.href.replace(path, path.replace('#', '%23'));
	return location.href;
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
			this.push('"');
			var s = term.value.toString();
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

});

