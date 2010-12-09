/*
   Copyright (c) 2009-2010 Zepheira LLC, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(initForms);

function initForms() {
	$("form[about]").each(function(i, node) {
		var form = $(node)
		form.bind("calliForm", function() {
			form.validate({submitHandler: submitRDFForm})
		})
	})
}

function submitRDFForm(form) {
	var form = $(form)
	var se = jQuery.Event("calliSubmit");
	form.trigger(se)
	if (!se.isDefaultPrevented()) {
		try {
			var added = readRDF(form)
			var type = "application/rdf+xml"
			var data = added.dump({format:"application/rdf+xml",serialize:true})
			postData(form, location.href, type, data, function(data, textStatus, xhr) {
				var redirect = xhr.getResponseHeader("Location")
				var event = jQuery.Event("calliRedirect")
				if (form.hasClass("diverted") || form.attr("data-diverted")) {
					event.location = window.calli.diverted(redirect, form.get(0))
				} else {
					event.location = redirect
				}
				form.trigger(event)
				if (!event.isDefaultPrevented()) {
					location.replace(event.location)
				}
			})
		} catch(e) {
			form.trigger("calliError", e.description)
		}
		return false
	}
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

function postData(form, url, type, data, callback) {
	var xhr = null
	xhr = $.ajax({ type: "POST", url: url, contentType: type, data: data, success: function(data, textStatus) {
		form.trigger("calliOk")
		callback(data, textStatus, xhr)
	}, error: function(xhr, textStatus, errorThrown) {
		form.trigger("calliError", [xhr.statusText ? xhr.statusText : errorThrown ? errorThrown : textStatus, xhr.responseText])
	}})
}

})(jQuery)

