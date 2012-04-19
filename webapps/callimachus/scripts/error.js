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

$(window).bind('message', function(event) {
	if ($('iframe').filter(function(){return this.contentWindow == event.originalEvent.source}).length) {
		var msg = event.originalEvent.data;
		if (msg.indexOf('ERROR ') == 0) {
			if (msg.indexOf('\n\n') > 0) {
				var message = msg.substring('ERROR '.length, msg.indexOf('\n\n'));
				var stack = msg.substring(msg.indexOf('\n\n') + 2);
				calli.error(message, stack);
			} else {
				calli.error(msg.substring('ERROR '.length));
			}
		}
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
	}
	if (!e.message) {
		e.message = asHtml(message);
	}
	if (typeof message == 'string' && stack && stack.indexOf('<') == 0) {
		e.message = $(stack).find("h1").andSelf().filter("h1").html();
		e.stack = $(stack).find("pre").andSelf().filter("pre").html();
	} else if (stack) {
		e.stack = asHtml(stack);
	}
	if (e.message) {
		try {
			$(document).trigger(e);
		} catch (e) {
			setTimeout(function(){throw e}, 0);
		}
	}
	var error;
	if (!message) {
		error = new Error();
	} else if (message instanceof Error) {
		error = message;
	} else if (typeof message == 'string') {
		error = new Error(message);
	} else if (message.nodeType) {
		error = new Error($('<p/>').append($(message).clone()).text());
	} else if (typeof message == 'function') {
		error = new Error(message.toSource());
	} else if (message.message) {
		error = new Error(message.message);
	} else {
		error = new Error(message.toString());
	}
	if (window.console && window.console.error) {
		console.error(error.message);
	}
	if (!e.isPropagationStopped() && parent) {
		if (stack) {
			parent.postMessage('ERROR ' + error.message + '\n\n' + stack, '*');
		} else {
			parent.postMessage('ERROR ' + error.message, '*');
		}
	}
	return error;
};

function asHtml(obj) {
	if (!obj) {
		return undefined;
	} else if (typeof obj == 'string') {
		return $('<p/>').text(obj).html();
	} else if (obj.nodeType) {
		return $('<p/>').append($(obj).clone()).html();
	} else if (typeof obj.toSource == 'function') {
		return $('<p/>').text(obj.toSource()).html();
	} else if (obj.message) {
		return $('<p/>').text(obj.message).html();
	} else {
		return $('<p/>').text(obj.toString()).html();
	}
}

})(jQuery, jQuery);

