// select-file.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

calli.selectFile = function(node) {
	var list = $(node).parents('[dropzone]');
	if (!list.length)
		return true;
	var title = '';
	if (list.attr("id")) {
		title = $("label[for='" + list.attr("id") + "']").text();
	}
	if (!title) {
		title = list.find("label").text();
	}
	var dialog = null;
	var onlinked = function() {
		calli.closeDialog(dialog);
	};
	var src = "/?view";
	var options = {
		onmessage: function(event) {
			if (event.data.indexOf('PUT src\n') == 0) {
				var data = event.data;
				src = data.substring(data.indexOf('\n\n') + 2);
			}
		},
		buttons: {
			"Select": function() {
				var uri = calli.listResourceIRIs(src)[0];
				var de = jQuery.Event('calliLink');
				de.location = uri;
				de.errorMessage = "Invalid Selection";
				$(node).trigger(de);
				calli.closeDialog(dialog);
			},
			"Cancel": function() {
				calli.closeDialog(dialog);
			}
		},
		onclose: function() {
			list.unbind('calliLinked', onlinked);
			node.focus();
		}
	};
	var openBrowseDialog = function(url) {
		dialog = calli.openDialog(url, title, options);
		list.bind('calliLinked', onlinked);
	};
	var url = null;
	if (window.sessionStorage) {
		try {
			var last = sessionStorage.getItem("LastFolder");
			if (last) {
				url = last;
			} else if (last = localStorage.getItem("LastFolder")) {
				url = last;
			}
		} catch (e) {
			// ignore
		}
	}
	if (url) {
		jQuery.ajax({
			type:"GET",
			url:url,
			complete:function(xhr) {
				if (xhr.status == 200 || xhr.status == 304) {
					openBrowseDialog(url);
				} else {
					openBrowseDialog("/?view");
				}
			}
		});
	} else {
		openBrowseDialog("/?view");
	}
	return false;
};

})(jQuery);

