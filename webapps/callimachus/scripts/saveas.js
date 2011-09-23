//saveas.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
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

window.calli.saveas = function(label, form, callback, fin) {
	if (typeof form == 'function') {
		fin = callback;
		callback = form;
		form = null;
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
				var uri = calli.listResourceIRIs(src)[0];
				var ns = uri;
				if (ns.lastIndexOf('/') != ns.length - 1) {
					ns += '/';
				}
				var local = encodeURI(label).replace(/%20/g,'+');
				updateFormAction(form, uri);
				callback(uri, label, ns, local);
				calli.closeDialog(dialog);
			}
		},
		onclose: function() {
			if (typeof fin == 'function') {
				fin();
			}
		}
	});
}

})(jQuery);

