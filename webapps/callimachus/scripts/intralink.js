// divert.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function handle(event) {
	select(event.target, "a.intralink,a[data-query]").each(function() {
		var href = window.calli.intralink(this.href, this);
		var link = $(this);
		if (this.href != href) {
			if (!link.attr("resource")) {
				link.attr("resource", link.attr("href"));
			}
			$(this).mousedown(function() {
				this.href = href;
			});
		}
		link.removeAttr("data-query");
		if (this.href.indexOf("/callimachus/go?q=") < 0) {
			link.removeClass("intralink");
		} else {
			link.addClass("intralink");
		}
	});
	select(event.target, "a.diverted,a[data-diverted]").each(function() {
		var href = window.calli.diverted(this.href, this);
		var link = $(this);
		if (this.href != href) {
			if (!link.attr("resource")) {
				link.attr("resource", link.attr("href"));
			}
			$(this).mousedown(function() {
				this.href = href;
			});
		}
		link.removeAttr("data-diverted");
		if (this.href.indexOf("/diverted;") < 0) {
			link.removeClass("diverted");
		} else {
			link.addClass("diverted");
		}
	});
}

if (!window.calli) {
	window.calli = {};
}

window.calli.intralink = function(url, node) {
    var prefix = location.protocol + '//' + location.host + '/';
	var query = $(node).attr("data-query");
	if (url.indexOf(prefix) != 0 && url.indexOf(':') > 0 || url.indexOf('?') > 0 || url.indexOf('#') > 0) {
		if (url.indexOf(':') < 0) {
		    if (node && node.baseURIObject && node.baseURIObject.resolve) {
		        url = node.baseURIObject.resolve(url);
		    } else {
		        var a = document.createElement('a');
		        a.setAttribute('href', url);
		        if (a.href) {
		            url = a.href;
		        }
		    }
		}
    	var path = 'callimachus/go?q=';
		url = prefix + path + encodeURIComponent(url);
		if (query) {
			url = url + '&query=' + encodeURIComponent(query);
		}
	} else if (query) {
		url = url + '?' + query;
	}
	return url;
}

window.calli.diverted = function(url, node) {
    var prefix = location.protocol + '//' + location.host + '/';
	if (url.indexOf(prefix) != 0 && url.indexOf(':') > 0 || url.indexOf('?') > 0 || url.indexOf('#') > 0) {
		if (url.indexOf(':') < 0) {
		    if (node && node.baseURIObject && node.baseURIObject.resolve) {
		        url = node.baseURIObject.resolve(url);
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
	var query = $(node).attr("data-diverted");
	if (query) {
	    url = url + query;
	}
	return url
}

})(window.jQuery);

