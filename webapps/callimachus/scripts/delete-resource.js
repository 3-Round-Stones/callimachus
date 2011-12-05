// delete-resource.js
/*
   Copyright (c) 2011 3 Round Stones Inc., Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

if (!window.calli) {
	window.calli = {};
}
window.calli.deleteResource = function(event, redirect) {
	var form = event.target ? event.target : event.srcElement ? event.srcElement : event;
	if (form.nodeType == 3) form = form.parentNode; // defeat Safari bug
	form = $(form);
	if(!form.is('form')) form = form.closest('form');

	if (event && !confirm("Are you sure you want to delete " + document.title + "?"))
		return;
	
	if (!form || !form.length) {
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
					event.location = redirect ? redirect : form.attr("data-redirect");
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
					if (event.location) {
						// TODO verify this location is not going to 404 on us w/o causing an calliError
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

function getLastModified() {
	try {
		var committedOn = $('#resource-lastmod').find('[property=audit:committedOn]').attr('content');
		return new Date(committedOn).toGMTString();
	} catch (e) {
		return null;
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

