/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(initForms);

function initForms() {
	$("form[about]").each(function(i, node) {
		var form = $(node);
		var stored = readRDF(form);
		form.bind("calliForm", function() {
			form.validate({submitHandler: function() {
				return submitRDFForm(form, stored);
			}})
		})
	})
}

function submitRDFForm(form, stored) {
	var se = jQuery.Event("calliSubmit");
	form.trigger(se);
	if (!se.isDefaultPrevented()) {
		try {
			var revised = readRDF(form);
			var removed = stored.except(revised);
			var added = revised.except(stored);
			removed.triples().each(function(){
				addBoundedDescription(this, stored, removed, added);
			})
			added.triples().each(function(){
				addBoundedDescription(this, revised, added, removed);
			})
			var boundary = "jeseditor-boundary";
			var type = "multipart/related;boundary=" + boundary + ";type=\"application/rdf+xml\"";
			var data = "--" + boundary + "\r\n" + "Content-Type: application/rdf+xml\r\n\r\n"
					+ removed.dump({format:"application/rdf+xml",serialize:true})
					+ "\r\n--" + boundary + "\r\n" + "Content-Type: application/rdf+xml\r\n\r\n"
					+ added.dump({format:"application/rdf+xml",serialize:true})
					+ "\r\n--" + boundary + "--";
			patchData(form, location.href, type, data, function(data, textStatus, xhr) {
				var redirect = xhr.getResponseHeader("Location");
				if (!redirect) {
					redirect = location.href;
					if (redirect.indexOf('?') > 0) {
						redirect = redirect.substring(0, redirect.indexOf('?'));
					}
					redirect = redirect + "?view";
				}
				var event = jQuery.Event("calliRedirect");
				event.location = redirect;
				form.trigger(event);
				if (!event.isDefaultPrevented()) {
					location.replace(event.location);
				}
			})
		} catch(e) {
			form.trigger("calliError", e.description ? e.description : e);
		}
	}
	return false
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

function patchData(form, url, type, data, callback) {
	var xhr = null
	xhr = $.ajax({ type: "POST", url: url, contentType: type, data: data, beforeSend: function(xhr){
		var etag = getEntityTag()
		if (etag) {
			xhr.setRequestHeader("If-Match", etag)
		}
	}, success: function(data, textStatus) {
		form.trigger("calliOk")
		callback(data, textStatus, xhr)
	}, error: function(xhr, textStatus, errorThrown) {
		form.trigger("calliError", [xhr.statusText ? xhr.statusText : errorThrown ? errorThrown : textStatus, xhr.responseText])
	}})
}

function getEntityTag() {
	var etag = null
	$("head>meta[http-equiv]").each(function(){
		if (this.attributes['http-equiv'].value == 'etag') {
			etag = this.attributes['content'].value
		}
	})
	return etag
}

if (!window.calli) {
	window.calli = {};
}
window.calli.deleteResource = function() {
	var form = $("form[about]")
	var url = location.href
	if (url.indexOf('?') > 0) {
		url = url.substring(0, url.indexOf('?'))
	}
	var xhr = $.ajax({ type: "DELETE", url: url, beforeSend: function(xhr){
		var etag = getEntityTag()
		if (etag) {
			xhr.setRequestHeader("If-Match", etag)
		}
	}, success: function(data, textStatus) {
		var event = jQuery.Event("calliRedirect")
		event.location = form.attr("data-redirect")
		if (form.hasClass("diverted")) {
			event.location = window.calli.diverted(event.location, form.get(0))
		}
		if (!event.location) {
			if (window.sessionStorage) {
				var previous = sessionStorage.getItem("Previous")
				if (previous) {
					event.location = previous.substring(0, previous.indexOf(' '))
				}
			}
		}
		if (!event.location) {
			event.location = document.referrer
		}
		form.trigger(event)
		if (!event.isDefaultPrevented()) {
			if (event.location) {
				location.replace(event.location)
			} else {
				history.go(-1)
			}
		}
	}, error: function(xhr, textStatus, errorThrown) {
		form.trigger("calliError", [xhr.statusText ? xhr.statusText : errorThrown ? errorThrown : textStatus, xhr.responseText])
	}})
}

})(jQuery)

