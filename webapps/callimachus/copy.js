/*
   Copyright (c) 2009-2010 Zepheira LLC, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

if (document.addEventListener) {
	document.addEventListener("DOMContentLoaded", initForms, false);
}  else if (window.attachEvent) {
    window.attachEvent("onload", initForms);
}

function initForms() {
	$("form[about]").each(function(i, node) {
		var form = $(node)
		form.submit(function(){
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
					if (redirect && redirect.indexOf("?") >= 0) {
						redirect = redirect
					} else if (redirect && location.href.indexOf("?pre-") > 0) {
						redirect = redirect + "?pre-view"
					} else if (redirect) {
						redirect = redirect + "?view"
					} else if (location.href.indexOf("?pre-") > 0) {
						redirect = uri + "?pre-view"
					} else {
						redirect = uri + "?view"
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
		})
	})
}

function readRDF(form) {
	var subj = $.uri.base()
	if ($(form).attr("about")) {
		subj = subj.resolve($(form).attr("about"))
	}
	var store = form.rdf().databank
	store.triples().each(function(){
		if (this.subject.type == 'uri' && this.subject.value.toString() != subj.toString()) {
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
