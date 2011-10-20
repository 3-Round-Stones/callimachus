// save-form-as.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

var originalSubmit = true;
function resubmit(form) {
	var previously = originalSubmit;
	originalSubmit = false;
	try {
		overrideLocation(form, $(form).attr('about'));
		$(form).submit(); // this time with an about attribute
	} finally {
		originalSubmit = previously;
	}
}
window.calli.saveFormAs = function(form, fileName, create) {
	$(form).find("input").change(); // IE may not have called onchange before onsubmit
	var about = $(form).attr('about');
	if (originalSubmit && fileName) { // prompt for a new URI
		openSaveAsDialog(form, fileName, create, function(ns, local){
			$(form).attr('about', ns + local.replace(/\+/g,'-').toLowerCase());
			resubmit(form);
		});
		return false;
	} else if (about && about.indexOf(':') < 0 && about.indexOf('/') != 0 && about.indexOf('?') != 0) {
		return promptIfNeeded(form, decodeURI(about), create, function(ns, local){
			$(form).attr('about', ns + local);
		});
	} else if (about) { // absolute about attribute already set
		overrideLocation(form, $(form).attr('about'));
		return true;
	} else { // no identifier at all
		var field = $($(form).find('input:not(:checkbox,:disabled,:button,:password,:radio)')[0]);
		var input = field.val();
		if (input) {
			field.change(function() {
				if (input != $(field).val()) {
					// restore the about attribute when this field changes
					if (about) {
						$(form).attr('about', about);
					} else {
						$(form).removeAttr('about');
					}
				}
			});
		}
		var label = input;
		if (label && label.lastIndexOf('\\') > 0) {
			label = label.substring(label.lastIndexOf('\\') + 1);
		}
		return promptIfNeeded(form, label, create, function(ns, local){
			$(form).attr('about', ns + local.toLowerCase());
		});
	}
};

function promptIfNeeded(form, label, create, callback) {
	if (label && label.search(/^[\w\.\-\_ ]*\/?$/) == 0 && location.search.search(/\?\w+=/) >= 0) {
		var ns = calli.listResourceIRIs(getPageLocationURL())[0];
		if (ns.lastIndexOf('/') != ns.length - 1) {
			ns += '/';
		}
		var local = encodeURI(label).replace(/%20/g, '+');
		callback(ns, local);
		overrideLocation(form, $(form).attr('about'));
		return true;
	} else {
		openSaveAsDialog(form, label, create, function(ns, local) {
			callback(ns, local);
			resubmit(form); // this time with an about attribute
		});
		return false;
	}
}

function openSaveAsDialog(form, label, create, callback) {
	var src = "/callimachus/pages/location-prompt.html#" + encodeURIComponent(label);
	if (location.search.search(/\?\w+=/) == 0) {
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
	var dialog = window.calli.openDialog(src, 'Save As...', {
		buttons: {
			"Save": function() {
				dialog.postMessage('GET label', '*');
			},
			"Cancel": function() {
				calli.closeDialog(dialog);
			}
		},
		onmessage: function(event) {
			if (event.data == 'POST save') {
				dialog.postMessage('OK\n\n' + event.data, '*');
				dialog.postMessage('GET label', '*');
			} else if (event.data.indexOf('OK\n\nGET label\n\n') == 0) {
				var data = event.data;
				label = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
				dialog.postMessage('GET url', '*');
			} else if (event.data.indexOf('OK\n\nGET url\n\n') == 0) {
				var data = event.data;
				var src = data.substring(data.indexOf('\n\n', data.indexOf('\n\n') + 2) + 2);
				var composite = calli.listResourceIRIs(src)[0];
				var ns = composite;
				if (ns.lastIndexOf('/') != ns.length - 1) {
					ns += '/';
				}
				var local = encodeURI(label).replace(/%20/g,'+');
				updateFormAction(form, composite, create);
				callback(ns, local);
				calli.closeDialog(dialog);
			}
		}
	});
	return dialog;
}

function updateFormAction(form, composite, create) {
	if (form && composite) {
		var m;
		var action = getFormAction(form);
		if (create) {
			form.action = composite + '?create=' + create;
		} else if (m = action.match(/^([^\?]*)\?create(&.*)?$/)) {
			action = composite + '?create=';
			if (create) {
				action += create;
			} else if (m[1]) {
				action += m[1];
			} else {
				action += location.pathname;
			}
			if (m[2]) {
				action += m[2];
			}
			form.action = action;
		} else if (m = action.match(/^([^\?]*)(\?create=[^&]+)(&.*)?$/)) {
			action = composite + m[2];
			if (m[3]) {
				action += m[3];
			}
			form.action = action;
		}
	}
}

function overrideLocation(form, uri) {
	var action = getFormAction(form);
	if (action.indexOf('&location=') > 0) {
		var m = action.match(/^(.*&location=)[^&=]*(.*)$/);
		form.action = m[1] + encodeURIComponent(uri) + m[2];
	} else {
		form.action = action + '&location=' + encodeURIComponent(uri);
	}
	if (action.indexOf('&intermediate=') < 0 && isIntermidate(action)) {
		form.action += '&intermediate=true';
	}
}

function isIntermidate(url) {
	if (window.parent != window) {
		try {
			var childUrl = url;
			if (childUrl.indexOf('?create') > 0) {
				childUrl = childUrl.substring(0, childUrl.indexOf('?'));
				var parentUrl = window.parent.location.href;
				if (parentUrl.indexOf('?edit') > 0) {
					parentUrl = parentUrl.substring(0, parentUrl.indexOf('?'));
					if (parentUrl == childUrl) {
						// they are creating a component in a dialog from an edit form
						return true;
					}
				}
			}
		} catch (e) {
			// I guess not
		}
	}
	return false;
}

function getFormAction(form) {
	if (form.getAttribute("action"))
		return form.action;
	var url = getPageLocationURL();
	if (url.indexOf('#') > 0)
		return url.substring(0, url.indexOf('#'));
	return url;
}

function getPageLocationURL() {
	// window.location.href needlessly decodes URI-encoded characters in the URI path
	// https://bugs.webkit.org/show_bug.cgi?id=30225
	var path = location.pathname;
	if (path.match(/#/))
		return location.href.replace(path, path.replace('#', '%23'));
	return location.href;
}

})(jQuery);

