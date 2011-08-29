//create.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

function getPageLocationURL() {
	// window.location.href needlessly decodes URI-encoded characters in the URI path
	// https://bugs.webkit.org/show_bug.cgi?id=30225
	var path = location.pathname;
	if (path.match(/#/))
		return location.href.replace(path, path.replace('#', '%23'));
	return location.href;
}

$(document).ready(initForms);

function initForms() {
	$('form[about],form[enctype="application/sparql-update"]').each(function(i, node) {
		var form = $(node);
		$(document).bind("calliReady", function() {
			form.validate({submitHandler: submitRDFForm});
		});
	});
	if (window.frameElement) {
		$('form').each(function(i, node) {
			if (!this.getAttribute("action")) {
				this.action = location.search + '&intermediate=true';
			}
		});
	}
	$('form[enctype="multipart/form-data"]').submit(function() {
		var form = $(this);
		var file = form.find('input[type=file]');
		if (file.length == 1) {
			var label = file.val().match(/[\\/]([^\\/]+)$/)[1]
			var uri = calli.listResourceIRIs(getPageLocationURL())[0] + '/' + encodeURIComponent(label).replace(/%20/g,'+').toLowerCase();
			if (this.action.indexOf('&location=') > 0) {
				var m = this.action.match(/^(.*&location=)[^&=]*(.*)$/);
				this.action = m[1] + encodeURIComponent(uri) + m[2];
			} else {
				this.action = this.action + '&location=' + encodeURIComponent(uri);
			}
		}
		return true;
	});
}

function submitRDFForm(form) {
	var form = $(form);
	try {
		var se = jQuery.Event("calliSubmit");
		form.trigger(se);
		if (!se.isDefaultPrevented()) {
			form.find("input").change(); // IE may not have called onchange before onsubmit
			var uri = getResourceUri(form);
			var added = readRDF(uri, form);
			var type = "application/rdf+xml";
			var data = added.dump({format:"application/rdf+xml",serialize:true,namespaces:form.xmlns()});
			postData(form.action, type, uri, data, function(data, textStatus, xhr) {
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

var overrideFormURI = false;

function getResourceUri(form) {
	if (overrideFormURI || !form.attr('about') || form.attr('about') == $('body').attr('about')) {
		overrideFormURI = true;
		var label = form.find('input:text').val();
		if (label) {
			var uri = calli.listResourceIRIs(getPageLocationURL())[0] + '/' + encodeURI(label).replace(/%20/g,'+').toLowerCase();
			form.attr('about', uri);
			return uri;
		}
	}
	return form.attr('about');
}

function readRDF(uri, form) {
	var subj = $.uri.base().resolve(uri);
	var store = form.rdf().databank;
	store.triples().each(function(){
		if (this.subject.type == 'uri' && this.subject.value.toString() != subj.toString() && this.subject.value.toString().indexOf(subj.toString() + '#') != 0 && (this.object.type != 'uri' || this.object.value.toString() != subj.toString() && this.object.value.toString().indexOf(subj.toString() + '#') != 0)) {
			store.remove(this);
		} else if (this.subject.type == "bnode") {
			var orphan = true;
			$.rdf({databank: store}).where("?s ?p " + this.subject).each(function (i, bindings, triples) {
				orphan = false;
			});
			if (orphan) {
				store.remove(this);
			}
		}
	});
	return store;
}

function postData(url, type, loc, data, callback) {
	var xhr = null;
	xhr = $.ajax({
		type: "POST",
		url: url,
		contentType: type,
		data: data,
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Location', loc);
		},
		success: function(data, textStatus) {
			callback(data, textStatus, xhr);
		}
	});
}

})(jQuery)

