// divert.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function handle(event) {
	select(event.target, "a.intralink,a[data-query],a.diverted,a[data-diverted]").each(function() {
		var href = window.calli.intralink(this.href, $(this).attr("data-query"));
		var link = $(this);
		if (this.href != href) {
			$(this).mousedown(function() {
				this.href = href;
				if (!link.attr("resource")) {
					link.attr("resource", link.attr("href"));
				}
			});
			link.addClass("intralink");
		} else {
			link.removeClass("intralink");
		}
		link.removeAttr("data-query");
		link.removeAttr("data-diverted");
		link.removeClass("diverted");
	});
	if (select(event.target, "a.diverted").length) {
		setTimeout(function(){ throw 'Use class="intralink" instead'; }, 0);
	}
	if (select(event.target, "a[data-diverted]").length) {
		setTimeout(function(){ throw 'Use data-query="view" instead'; }, 0);
	}
}

if (!window.calli) {
	window.calli = {};
}

window.calli.intralink = function(uri, query) {
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
	setTimeout(function(){ throw "Use window.calli.intralink(uri, query) instead"; }, 0);
	var query = null;
	if ($(node).attr("data-diverted")) {
		 query = $(node).attr("data-diverted").substring(1);
	}
	return window.calli.intralink(url, query);
}

})(window.jQuery);

