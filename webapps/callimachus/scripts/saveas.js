// saveas.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

window.calli.saveas = function(form, fileName, create) {
	$(form).find("input").change(); // IE may not have called onchange before onsubmit
	var about = $(form).attr('about');
	if (about && about.indexOf(':') < 0 && about.indexOf('/') != 0 && about.indexOf('?') != 0) {
		promptLocation(form, decodeURI(about), create, function(ns, local){
			$(form).attr('about', ns + local);
			$(form).submit(); // this time with an about attribute
		});
		return false;
	} else if (about) { // absolute about attribute already set
		return true;
	} else if (fileName) { // no identifier at all
		promptLocation(form, fileName, create, function(ns, local){
			$(form).attr('about', ns + local.replace(/\+/g,'-').toLowerCase());
			$(form).submit(); // this time with an about attribute
		});
		return false;
	} else { // no identifier at all
		var field = $($(form).find('input')[0]);
		var input = field.val();
		if (input) {
			field.change(function() {
				if (input != $(form).find('input').val()) {
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
		promptLocation(form, label, create, function(ns, local){
			$(form).attr('about', ns + local.toLowerCase());
			$(form).submit(); // this time with an about attribute
		});
		return false;
	}
};

function promptLocation(form, label, create, callback) {
	if (label && label.search(/^[\w\.\-\_ ]*\/?$/) == 0 && location.search.search(/\?\w+=/) >= 0) {
		var ns = calli.listResourceIRIs(getPageLocationURL())[0];
		if (ns.lastIndexOf('/') != ns.length - 1) {
			ns += '/';
		}
		var local = encodeURI(label).replace(/%20/g, '+');
		callback(ns, local);
	} else {
		openSaveAsDialog(label, form, create, callback);
	}
}

function openSaveAsDialog(label, form, create, callback) {
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
		var action = form.action ? form.action : getPageLocationURL();
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

function getPageLocationURL() {
	// window.location.href needlessly decodes URI-encoded characters in the URI path
	// https://bugs.webkit.org/show_bug.cgi?id=30225
	var path = location.pathname;
	if (path.match(/#/))
		return location.href.replace(path, path.replace('#', '%23'));
	return location.href;
}

})(jQuery);

