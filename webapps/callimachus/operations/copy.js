/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
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
				try {
					if (window.parent && parent.calli && window.frameElement) {
						var subj = $.uri.base().resolve($(form).attr("about"))
						parent.calli.resourceCreated(subj.toString(), window.frameElement)
					}
				} catch (e) {}
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
			form.trigger("calliError", e.description ? e.description : e)
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
		var status = xhr.statusText ? xhr.statusText : errorThrown ? errorThrown : textStatus
		if (xhr.status == 409) { // Resource already exists
			var link = form.attr("about")
			if (form.hasClass("diverted") || form.attr("data-diverted")) {
				link = window.calli.diverted(link, form.get(0))
			}
			form.trigger("calliError", [status, xhr.responseText, link])
		} else {
			form.trigger("calliError", [status, xhr.responseText])
		}
	}})
}

})(jQuery)

