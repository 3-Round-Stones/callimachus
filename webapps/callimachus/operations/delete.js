/*
   Copyright (c) 2009-2010 Zepheira LLC, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(initForms);

function initForms() {
	$("form[about]").each(function(i, node) {
		var form = $(node)
		form.submit(function(event){
			setTimeout(function() { submitRDFForm(form) }, 100)
			event.preventDefault()
		})
	})
}

function submitRDFForm(form) {
	form.triggerHandler("calli:submit")
	try {
		var added = readRDF(form)
		var type = "application/rdf+xml"
		var data = added.dump({format:"application/rdf+xml",serialize:true})
		deleteData(form, location.href, type, data, function(data, textStatus, xhr) {
			if (form.attr("data-redirect")) {
				form.triggerHandler("calli:redirect")
				var redirect = form.attr("data-redirect")
				if (form.hasClass("diverted")) {
					location.replace(window.calli.diverted(redirect, node))
				} else {
					location.replace(redirect)
				}
			}
			form.remove();
		})
	} catch(e) {
		form.triggerHandler("calli:error", e.description)
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
		if (this.subject.type == 'uri'
				&& this.subject.value.toString() != subj.toString()
				&& this.object.value.toString() != subj.toString()
				&& this.subject.value.toString().indexOf(subj.toString() + '#') != 0
				&& this.object.value.toString().indexOf(subj.toString() + '#') != 0) {
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

function deleteData(form, url, type, data, callback) {
	var xhr = null
	xhr = $.ajax({ type: "DELETE", url: url, contentType: type, data: data, beforeSend: function(xhr){
		var etag = getEntityTag()
		if (etag) {
			xhr.setRequestHeader("If-Match", etag)
		}
	}, success: function(data, textStatus) {
		form.triggerHandler("calli:ok")
		callback(data, textStatus, xhr)
	}, error: function(xhr, textStatus, errorThrown) {
		form.triggerHandler("calli:error", [xhr.statusText ? xhr.statusText : errorThrown ? errorThrown : textStatus, xhr.responseText])
	}})
}

function getEntityTag() {
	var etag = null
	$("head>meta").each(function(){
		if (this.attributes['http-equiv'].value == 'etag') {
			etag = this.attributes['content'].value
		}
	})
	return etag
}

})(jQuery)

