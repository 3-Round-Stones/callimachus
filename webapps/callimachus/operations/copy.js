/*
   Copyright (c) 2009-2010 Zepheira LLC, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

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
	if (window.showRequest) {
		showRequest()
	}
	try {
		var added = readRDF(form)
		var type = "application/rdf+xml"
		var data = added.dump({format:"application/rdf+xml",serialize:true})
		postData(location.href, type, data, function(data, textStatus, xhr) {
			var uri = location.href
			if (uri.indexOf('?') > 0) {
				uri = uri.substring(0, uri.indexOf('?'))
			}
			var redirect = xhr.getResponseHeader("Location")
			if (form.attr("data-redirect")) {
				redirect = form.attr("data-redirect")
			} else if (redirect && redirect.indexOf("?") >= 0) {
				redirect = redirect
			} else if (redirect) {
				redirect = redirect + "?view"
			} else {
				redirect = uri + "?view"
			}
			if (window.showPageLoading) {
				showPageLoading()
			}
			if (window.diverted && form.hasClass("diverted")) {
				location.replace(diverted(redirect, node))
			} else {
				location.replace(redirect)
			}
		})
	} catch(e) {
		if (window.showError) {
			showError(e.description)
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

function postData(url, type, data, callback) {
	var xhr = null
	xhr = $.ajax({ type: "POST", url: url, contentType: type, data: data, success: function(data, textStatus) {
		if (window.showSuccess) {
			showSuccess()
		}
		callback(data, textStatus, xhr)
	}, error: function(xhr, textStatus, errorThrown) {
		if (window.showError) {
			showError(xhr.statusText ? xhr.statusText : errorThrown ? errorThrown : textStatus, xhr.responseText)
		}
	}})
}
