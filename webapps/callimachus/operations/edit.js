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
		var stored = readRDF(form);
		$(document).bind("calliReady", function() {
			form.validate({submitHandler: function() {
				return submitRDFForm(form, stored);
			}});
		});
	});
}

function submitRDFForm(form, stored) {
	var se = jQuery.Event("calliSubmit");
	form.trigger(se);
	if (!se.isDefaultPrevented()) {
		try {
			form.find("input").change(); // IE may not have called onchange before onsubmit
			var revised = readRDF(form);
			var removed = stored.except(revised);
			var added = revised.except(stored);
			removed.triples().each(function(){
				addBoundedDescription(this, stored, removed, added);
			})
			added.triples().each(function(){
				addBoundedDescription(this, revised, added, removed);
			})
			var boundary = "jeseditor-boundary";
			var type = "multipart/related;boundary=" + boundary + ";type=\"application/rdf+xml\"";
			var data = "--" + boundary + "\r\n" + "Content-Type: application/rdf+xml\r\n\r\n"
					+ removed.dump({format:"application/rdf+xml",serialize:true})
					+ "\r\n--" + boundary + "\r\n" + "Content-Type: application/rdf+xml\r\n\r\n"
					+ added.dump({format:"application/rdf+xml",serialize:true})
					+ "\r\n--" + boundary + "--";
			patchData(form, getPageLocationURL(), type, data, function(data, textStatus, xhr) {
				try {
					var redirect = xhr.getResponseHeader("Location");
					if (!redirect) {
						redirect = getPageLocationURL();
						if (redirect.indexOf('?') > 0) {
							redirect = redirect.substring(0, redirect.indexOf('?'));
						}
					}
					redirect = redirect + "?view";
					var event = jQuery.Event("calliRedirect");
					event.location = redirect;
					form.trigger(event);
					if (!event.isDefaultPrevented()) {
						location.replace(event.location);
					}
				} catch(e) {
					form.trigger("calliError", e.description ? e.description : e);
				}
			})
		} catch(e) {
			form.trigger("calliError", e.description ? e.description : e);
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
		if (this.subject.type == 'uri' && this.subject.value.toString() != subj.toString() && this.subject.value.toString().indexOf(subj.toString() + '#') != 0 && (this.object.type != 'uri' || this.object.value.toString() != subj.toString() && this.object.value.toString().indexOf(subj.toString() + '#') != 0)) {
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

function addBoundedDescription(triple, store, dest, copy) {
	if (triple.subject.type == "bnode") {
		var bnode = triple.subject
		$.rdf({databank: store}).where("?s ?p " + bnode).each(function (i, bindings, triples) {
			if (addTriple(triples[0], dest)) {
				copy.add(triples[0])
				addBoundedDescription(triples[0], store, dest, copy)
			}
		})
	}
	if (triple.object.type == "bnode") {
		var bnode = triple.object
		$.rdf({databank: store}).where(bnode + ' ?p ?o').each(function (i, bindings, triples) {
			if (addTriple(triples[0], dest)) {
				copy.add(triples[0])
				addBoundedDescription(triples[0], store, dest, copy)
			}
		})
	}
}

function addTriple(triple, store) {
	var size = store.size()
	store.add(triple)
	return store.size() > size
}

function patchData(form, url, type, data, callback) {
	var xhr = null;
	xhr = $.ajax({ type: "POST", url: url, contentType: type, data: data, beforeSend: function(xhr){
		var lastmod = getLastModified();
		if (lastmod) {
			xhr.setRequestHeader("If-Unmodified-Since", lastmod);
		}
	}, success: function(data, textStatus) {
		callback(data, textStatus, xhr);
	}})
}

function getLastModified() {
	try {
		var committedOn = $('#footer-lastmod').find('[property=audit:committedOn]').attr('content');
		return new Date(committedOn).toGMTString();
	} catch (e) {
		return null;
	}
}

if (!window.calli) {
	window.calli = {};
}
window.calli.deleteResource = function(form) {
	if (form && !confirm("Are you sure you want to delete " + document.title + "?"))
		return;
	form = $(form);
	if (!form.length) {
		form = $("form[about]");
	}
	if (!form.length) {
		form = $(document);
	}
	try {
		var de = jQuery.Event("calliDelete");
		form.trigger(de);
		if (!de.isDefaultPrevented()) {
			var url = getPageLocationURL();
			if (url.indexOf('?') > 0) {
				url = url.substring(0, url.indexOf('?'));
			}
			var xhr = $.ajax({ type: "DELETE", url: url, beforeSend: function(xhr){
				var lastmod = getLastModified();
				if (lastmod) {
					xhr.setRequestHeader("If-Unmodified-Since", lastmod);
				}
			}, success: function(data, textStatus) {
				try {
					var event = jQuery.Event("calliRedirect");
					event.location = form.attr("data-redirect");
					if (!event.location) {
						if (window.sessionStorage) {
							var previous = sessionStorage.getItem("Previous");
							if (previous) {
								event.location = previous.substring(0, previous.indexOf(' '));
							}
						}
						if (!event.location) {
							event.location = document.referrer;
						}
						if (event.location) {
							var href = getPageLocationURL();
							if (href.indexOf('?') > 0) {
								href = href.substring(0, href.indexOf('?'));
							}
							var referrer = event.location;
							if (referrer.indexOf('?') > 0) {
								referrer = referrer.substring(0, referrer.indexOf('?'));
							}
							if (href == referrer) {
								event.location = null; // don't redirect back to self
							}
						}
					}
					if (!event.location) {
						event.location = location.protocol + '//' + location.host + '/';
					}
					form.trigger(event)
					if (!event.isDefaultPrevented()) {
						if (event.location) {
							location.replace(event.location)
						} else {
							history.go(-1)
						}
					}
				} catch(e) {
					form.trigger("calliError", e.description ? e.description : e);
				}
			}})
		}
	} catch(e) {
		form.trigger("calliError", e.description ? e.description : e);
	}
}

})(jQuery)

