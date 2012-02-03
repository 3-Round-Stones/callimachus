// error.js

(function($, jQuery){

if (!window.calli) {
	window.calli = {};
}

var unloading = false;
$(window).bind('beforeunload', function() {
	unloading = true;
});

$(document).ajaxError(function(event, xhr, ajaxOptions, thrownError){
	if (xhr && xhr.status >= 400) {
		calli.error(xhr.statusText, xhr.responseText);
	} else if (thrownError) {
		calli.error(thrownError);
	} else if (xhr && xhr.status < 100) {
		setTimeout(function() {
			if (!unloading) {
				calli.error("Could not connect to server, please try again later");
			}
		}, 0);
	}
});


// calli.error("message");
// calli.error("message", "stack");
// calli.error("message", "<html/>");
// calli.error(<span/>, "stack");
// calli.error(caught);
// calli.error({message:getter,stack:getter});
// calli.error({description:getter});
window.calli.error = function(message, stack) {
	setTimeout(function() {
		var e = jQuery.Event("error");
		if (typeof message == 'object') {
			if (message.description) {
				e.message = asHtml(message.description);
			}
			if (message.name) {
				e.name = asHtml(message.name);
			}
			if (message.message) {
				e.message = asHtml(message.message);
			}
			if (message.stack) {
				e.stack = asHtml(message.stack);
			}
		} else {
			e.message = asHtml(message);
		}
		if (typeof message == 'string' && stack && stack.indexOf('<') == 0) {
			e.message = $(stack).find("h1").andSelf().filter("h1").html();
			e.stack = $(stack).find("pre").andSelf().filter("pre").html();
		} else if (stack) {
			e.stack = asHtml(stack);
		}
		if (e.message) {
			$(document).trigger(e);
		}
	}, 0);
	if (!message)
		return undefined;
	if (message instanceof Error)
		return message;
	if (typeof message == 'string')
		return new Error(message);
	if (message.nodeType)
		return new Error($('<p/>').append(message).text());
	if (typeof message == 'function')
		return new Error(message.toSource());
	if (message.message)
		return new Error(message.message);
	return new Error(message.toString());
};

function asHtml(obj) {
	if (!obj) {
		return undefined;
	} else if (typeof obj == 'string') {
		return $('<p/>').text(obj).html();
	} else if (obj.nodeType) {
		return $('<p/>').append(obj).html();
	} else if (typeof obj.toSource == 'function') {
		return $('<p/>').text(obj.toSource()).html();
	} else if (obj.message) {
		return $('<p/>').text(obj.message).html();
	} else {
		return $('<p/>').text(obj.toString()).html();
	}
}

})(jQuery, jQuery);

