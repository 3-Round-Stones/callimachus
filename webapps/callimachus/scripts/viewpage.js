// viewpage.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function handle(event) {
	select(event.target, "a.view,a.diverted,a[data-diverted]").each(function() {
		var href = window.calli.viewpage(this.href, $(this).attr("data-query"));
		var link = $(this);
		if (this.href != href) {
			var resource = link.attr("href");
			$(this).mousedown(function() {
				if (!link.attr("resource")) {
					link.attr("resource", resource);
				}
				this.href = href;
			});
			$(this).bind('dragstart', function(event) {
				var e = event.originalEvent;
				e.dataTransfer.setData('text/uri-list', resource);
				e.dataTransfer.setData('text/plain', resource);
			});
			link.addClass("view");
		} else {
			link.removeClass("view");
		}
		link.removeAttr("data-diverted");
		link.removeClass("diverted");
	});
	if (select(event.target, "a.diverted").length || select(event.target, "a[data-diverted]").length) {
		setTimeout(function(){ throw 'Use class="view" instead'; }, 0);
	}
}

if (!window.calli) {
	window.calli = {};
}

window.calli.viewpage = function(uri) {
	var url = uri;
	if (url.indexOf(':') < 0) {
	    if (document.baseURIObject && document.baseURIObject.resolve) {
	        url = document.baseURIObject.resolve(url);
	    } else {
	        var a = document.createElement('a');
	        a.setAttribute('href', url);
	        if (a.href) {
	            url = a.href;
	        }
	    }
	}
    var prefix = location.protocol + '//' + location.host + '/';
	if (url.indexOf(prefix) == 0) {
		url = url.substring(prefix.length - 1);
	}
	return prefix + 'callimachus/view?iri=' + encodeURIComponent(url).replace(/%2F/g, '/').replace(/%3A/g, ':');
}

window.calli.diverted = function(uri, query) {
	var url = uri;
    var prefix = location.protocol + '//' + location.host + '/';
	if (url.indexOf(prefix) != 0 && url.indexOf(':') > 0 || url.indexOf('?') > 0 || url.indexOf('#') > 0) {
		if (url.indexOf(':') < 0) {
		    if (document.baseURIObject && document.baseURIObject.resolve) {
		        url = document.baseURIObject.resolve(url);
		    } else {
		        var a = document.createElement('a');
		        a.setAttribute('href', url);
		        if (a.href) {
		            url = a.href;
		        }
		    }
		}
    	var path = 'diverted;';
		url = prefix + path + encodeURIComponent(url);
	}
	if (typeof query == "string") {
		url = url + '?' + query;
	}
	return url;
}

})(window.jQuery);

