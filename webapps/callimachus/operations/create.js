//create.js
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
		$(document).bind("calliReady", function() {
			form.validate({submitHandler: submitRDFForm})
		})
	})
}

function submitRDFForm(form) {
	var form = $(form);
	try {
		var se = jQuery.Event("calliSubmit");
		form.trigger(se);
		if (!se.isDefaultPrevented()) {
			form.find("input").change(); // IE may not have called onchange before onsubmit
			var added = readRDF(form);
			var type = "application/rdf+xml";
			var data = added.dump({format:"application/rdf+xml",serialize:true});
			postData(form, location.href, type, data, function(data, textStatus, xhr) {
				try {
					var redirect = xhr.getResponseHeader("Location");
					try {
						if (window.frameElement && parent.jQuery) {
							var ce = parent.jQuery.Event("calliCreate");
							ce.location = window.calli.viewpage(redirect);
							ce.about = $.uri.base().resolve($(form).attr("about")).toString();
							ce.rdfType = $(form).attr("typeof");
							parent.jQuery(frameElement).trigger(ce);
							if (ce.isDefaultPrevented()) {
								return;
							}
						}
					} catch (e) { }
					var event = jQuery.Event("calliRedirect");
					event.location = window.calli.viewpage(redirect);
					form.trigger(event);
					if (!event.isDefaultPrevented()) {
						location.replace(event.location);
					}
				} catch(e) {
					form.trigger("calliError", e.description ? e.description : e);
				}
			})
		}
	} catch(e) {
		form.trigger("calliError", e.description ? e.description : e);
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

function postData(form, url, type, data, callback) {
	var xhr = null
	xhr = $.ajax({ type: "POST", url: url, contentType: type, data: data, success: function(data, textStatus) {
		callback(data, textStatus, xhr)
	}})
}

})(jQuery)

