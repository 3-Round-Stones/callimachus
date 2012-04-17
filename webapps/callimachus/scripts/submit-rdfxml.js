// submit-rdfxml.js
/*
   Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

jQuery(function($){

$('form[enctype="application/rdf+xml"]').submit(function(event) {
	var form = $(this);
	form.find("input").change(); // IE may not have called onchange before onsubmit
	var about = form.attr('about');
	if (!about || about.indexOf(':') < 0 && about.indexOf('/') != 0 && about.indexOf('?') != 0)
		return true; // about attribute not set yet
	event.preventDefault();
	setTimeout(function(){submitRDFForm(form[0], form.attr('about'));}, 0);
	return false;
});

function submitRDFForm(form, uri) {
	try {
		var added = readRDF(uri, form);
		var data = added.dump({format:"application/rdf+xml",serialize:true,namespaces:$(form).xmlns()});
		postData(form, data, function(data, textStatus, xhr) {
			try {
				var redirect = xhr.getResponseHeader("Location");
				if (!redirect) {
					redirect = calli.getPageUrl();
					if (redirect.indexOf('?') > 0) {
						redirect = redirect.substring(0, redirect.indexOf('?'));
					}
				}
				var event = $.Event("calliRedirect");
				event.location = window.calli.viewpage(redirect);
				$(form).trigger(event);
				if (!event.isDefaultPrevented()) {
					if (window.parent != window && parent.postMessage) {
						parent.postMessage('PUT src\n\n' + event.location, '*');
					}
					window.location.replace(event.location);
				}
			} catch(e) {
				throw calli.error(e);
			}
		});
	} catch(e) {
		throw calli.error(e);
	}
}

function readRDF(uri, form) {
	var subj = $.uri.base().resolve(uri);
	var store = $(form).rdf().databank;
	store.triples().each(function(){
		if (this.subject.type == 'uri' && this.subject.value.toString() != subj.toString() && this.subject.value.toString().indexOf(subj.toString() + '#') != 0) {
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

function postData(form, data, callback) {
	var method = form.getAttribute('method');
	if (!method) {
		method = form.method;
	}
	if (!method) {
		method = "POST";
	}
	var type = form.getAttribute("enctype");
	if (!type) {
		type = "application/rdf+xml";
	}
	var xhr = null;
	xhr = $.ajax({
		type: method,
		url: form.action,
		contentType: type,
		data: data,
		success: function(data, textStatus) {
			callback(data, textStatus, xhr);
		}
	});
}

});

