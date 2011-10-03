// submit-rdfxml.js
/*
   Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){

$('form[method="POST"][enctype="application/rdf+xml"]').submit(function(event) {
	var form = $(this);
	form.find("input").change(); // IE may not have called onchange before onsubmit
	var about = form.attr('about');
	if (!about || about.indexOf(':') < 0 && about.indexOf('/') != 0 && about.indexOf('?') != 0)
		return true; // about attribute not set yet
	event.preventDefault();
	submitRDFForm(this, about, form.attr('enctype'));
	return false;
});

function submitRDFForm(form, uri, type) {
	try {
		var added = readRDF(uri, form);
		var data = added.dump({format:type,serialize:true,namespaces:$(form).xmlns()});
		postData(form.action, type, uri, data, function(data, textStatus, xhr) {
			try {
				var redirect = xhr.getResponseHeader("Location");
				var event = jQuery.Event("calliRedirect");
				event.location = window.calli.viewpage(redirect);
				$(form).trigger(event);
				if (!event.isDefaultPrevented()) {
					window.location.replace(event.location);
				}
			} catch(e) {
				$(form).trigger("calliError", e.description ? e.description : e);
			}
		});
	} catch(e) {
		$(form).trigger("calliError", e.description ? e.description : e);
	}
}

function readRDF(uri, form) {
	var subj = $.uri.base().resolve(uri);
	var store = $(form).rdf().databank;
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
		success: function(data, textStatus) {
			callback(data, textStatus, xhr);
		}
	});
}

});

