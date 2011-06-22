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
	$("form[about]").each(function(i, node) {
		var form = $(node);
		$(document).bind("calliReady", function() {
			form.validate({submitHandler: submitRDFForm});
		});
	});
	$('form[enctype="multipart/form-data"]:not([target])').each(function(i, node) {
		var form = $(this);
		var id = form.attr('id');
		if (!id) {
			id = 'form-data' + i;
		}
		form.attr('target', id + '-iframe');
		if (window.frameElement && this.action) {
			this.action = this.action + '&intermediate=true';
		} else if (window.frameElement) {
			this.action = getPageLocationURL() + '&intermediate=true';
		}
		var iframe = $('<iframe/>');
		iframe.attr('id', "iframe");
		iframe.attr('name', id + '-iframe');
		iframe.attr('src', "about:blank");
		iframe.attr('style', "display:none");
		$('body').append(iframe);
		iframe.load(function() {
			var redirect = $(this.contentWindow.document).text();
			if (redirect && redirect.indexOf('http') == 0) {
				try {
					if (window.frameElement && parent.jQuery) {
						var ce = parent.jQuery.Event("calliCreate");
						ce.location = window.calli.viewpage(redirect);
						ce.about = redirect;
						ce.rdfType = "foaf:Image";
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
			} else {
				form.trigger("calliError", $(this.contentWindow.document).find('h1').html(), $(this.contentWindow.document).find('pre').html());
			}
		});
	});
}

function submitRDFForm(form) {
	var form = $(form);
	try {
		var se = jQuery.Event("calliSubmit");
		form.trigger(se);
		if (!se.isDefaultPrevented()) {
			form.find("input").change(); // IE may not have called onchange before onsubmit
			if (form.attr('about') == $('body').attr('about')) {
				var label = form.find('input:text').val();
				if (label) {
					form.attr('about', $('body').attr('about') + '/' + encodeURI(label).replace(/%20/g,'+').toLowerCase());
				}
			}
			var added = readRDF(form);
			var type = "application/rdf+xml";
			var data = added.dump({format:"application/rdf+xml",serialize:true});
			postData(form, type, data, function(data, textStatus, xhr) {
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
	var subj = $.uri.base();
	if ($(form).attr("about")) {
		subj = subj.resolve($(form).attr("about"));
	}
	var store = form.rdf().databank;
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

function postData(form, type, data, callback) {
	var url = getPageLocationURL();
	if (window.frameElement) {
		url = location.search + '&intermediate=true';
	}
	var xhr = null;
	xhr = $.ajax({ type: "POST", url: url, contentType: type, data: data, success: function(data, textStatus) {
		callback(data, textStatus, xhr);
	}});
}

})(jQuery)

