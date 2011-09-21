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
	$('form[enctype="application/sparql-update"]').each(function(i, node) {
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
	var overrideLocationURI = false;
	var prompted = false;
	$('form[enctype="multipart/form-data"]').submit(function(event) {
		if (prompted)
			return true;
		var form = $(this);
		if (overrideLocationURI || this.action.indexOf('&location=') < 0) {
			event.preventDefault();
			prompted = true;
			getResourceUri(this, function(uri){
				overrideLocation(form[0], uri);
				overrideLocationURI = true;
				form.submit();
			}, function(){
				prompted = false;
			});
			return false;
		}
		return true;
	});
}

function overrideLocation(form, uri) {
	if (form.action.indexOf('&location=') > 0) {
		var m = form.action.match(/^(.*&location=)[^&=]*(.*)$/);
		form.action = m[1] + encodeURIComponent(uri) + m[2];
	} else {
		form.action = form.action + '&location=' + encodeURIComponent(uri);
	}
}

function submitRDFForm(form) {
	var se = jQuery.Event("calliSubmit");
	$(form).trigger(se);
	if (!se.isDefaultPrevented()) {
		$(form).find("input").change(); // IE may not have called onchange before onsubmit
		getResourceUri(form, function(uri){
			try {
				var added = readRDF(uri, form);
				var type = "application/rdf+xml";
				var data = added.dump({format:"application/rdf+xml",serialize:true,namespaces:$(form).xmlns()});
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
		});
	}
	return false;
}

var overrideFormURI = false;
var overrideFormLocation = false;

function getResourceUri(form, callback, fin) {
	var uri = $(form).attr('about');
	if (overrideFormURI || !uri || uri == $('body').attr('about')) {
		overrideFormURI = true;
		var input = $(form).find('input').val();
		if (input.lastIndexOf('\\') > 0) {
			input = input.substring(input.lastIndexOf('\\') + 1);
		}
		promptLocation(form, input, function(ns, label){
			var uri = ns + encodeURI(label).replace(/%20/g,'+').toLowerCase();
			$(form).attr('about', uri);
			callback(uri);
		}, fin);
	} else if (overrideFormLocation || (uri.indexOf(':') < 0 && uri.indexOf('/') != 0 && uri.indexOf('?') != 0)) {
		overrideFormLocation = true;
		promptLocation(form, decodeURI(uri), function(ns, label){
			var uri = ns + encodeURI(label).replace(/%20/g,'+');
			$(form).attr('about', uri);
			callback(uri);
		}, fin);
	} else {
		callback($(form).attr('about'));
		if (typeof fin == 'function') {
			fin();
		}
	}
}

function promptLocation(form, label, callback, fin) {
	if (label && location.search.search(/\?\w+=/) >= 0) {
		var ns = calli.listResourceIRIs(getPageLocationURL())[0];
		if (ns.lastIndexOf('/') != ns.length - 1) {
			ns += '/';
		}
		callback(ns, label);
		if (typeof fin == 'function') {
			fin();
		}
	} else {
		calli.promptLocation(form, label, callback, fin);
	}
}

function updateFormAction(form, ns) {
	if (form) {
		var m;
		var action = form.action ? form.action : getPageLocationURL();
		if (m = action.match(/^([^\?]*)(\?\w+)(&.*)?$/)) {
			action = ns + m[2] + '=';
			if (m[1]) {
				action += m[1];
			} else {
				action += location.pathname;
			}
			if (m[3]) {
				action += m[3];
			}
			form.action = action;
		} else if (m = action.match(/^([^\?]*)(\?\w+=[^&]+)(&.*)?$/)) {
			action = ns + m[2];
			if (m[3]) {
				action += m[3];
			}
			form.action = action;
		}
	}
}

window.calli.promptLocation = function(form, label, callback, fin) {
	var width = 450;
	var height = 500;
	if ($('html').is('.iframe')) {
		width = 350;
		height = 450;
	}
	var src = "/callimachus/pages/location-prompt.html#" + encodeURIComponent(label);
	if (location.search.search(/\?\w+=/) >= 0) {
		src += '!' + calli.listResourceIRIs(getPageLocationURL())[0];
	} else if (window.sessionStorage) {
		try {
			var url = sessionStorage.getItem("LastFolder");
			if (url) {
				src += '!' + calli.listResourceIRIs(url)[0];
			} else if (url = localStorage.setItem("LastFolder")) {
				src += '!' + calli.listResourceIRIs(url)[0];
			}
		} catch (e) {
			// ignore
		}
	}
	var iframe = $("<iframe></iframe>");
	iframe.attr('src', src);
	iframe.dialog({
		title: 'Save As...',
		autoOpen: false,
		modal: false,
		draggable: true,
		resizable: true,
		autoResize: true,
		width: width,
		height: height,
		buttons: {
			"OK": function() {
				iframe[0].contentWindow.postMessage('GET label', '*');
			},
			"Cancel": function() {
				iframe.dialog('close');
			}
		}
	});
	var handle = function(event) {
		if (event.originalEvent.source == iframe[0].contentWindow && event.originalEvent.data.indexOf('OK\n\nGET label\n\n') == 0) {
			var data = event.originalEvent.data;
			label = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
			iframe[0].contentWindow.postMessage('GET url', '*');
		} else if (event.originalEvent.source == iframe[0].contentWindow && event.originalEvent.data.indexOf('OK\n\nGET url\n\n') == 0) {
			var data = event.originalEvent.data;
			var src = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
			var uri = calli.listResourceIRIs(src)[0];
			if (uri.lastIndexOf('/') != uri.length - 1) {
				uri += '/';
			}
			updateFormAction(form, uri);
			callback(uri, label);
			iframe.dialog('close');
		}
	};
	$(window).bind('message', handle);
	iframe.bind("dialogclose", function(event, ui) {
		$(window).unbind('message', handle);
		iframe.remove();
		iframe.parent().remove();
		if (typeof fin == 'function') {
			fin();
		}
	});
	iframe.dialog("open");
	iframe.css('width', '100%');
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
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Location', loc);
		},
		success: function(data, textStatus) {
			callback(data, textStatus, xhr);
		}
	});
}

})(jQuery)

