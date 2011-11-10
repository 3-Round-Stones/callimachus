// error.js

(function($){

var unloading = false;
$(window).bind('beforeunload', function() {
	unloading = true;
});

$(document).ajaxError(function(event, xhr, ajaxOptions, thrownError){
	if (xhr && xhr.status >= 400) {
		showError(event, xhr.statusText, xhr.responseText);
	} else if (thrownError) {
		showError(event, thrownError);
	} else if (xhr && xhr.status < 100) {
		setTimeout(function() {
			if (!unloading) {
				showError(event, "Could not connect to server, please try again later");
			}
		}, 0);
	}
});

$(document).ajaxSuccess(function(event, xhr, ajaxOptions){
	if (xhr && xhr.status >= 200 && xhr.status < 300) {
		$(document).trigger('calliSuccess');
	}
});

$(document).ready(function() {
	$('form').bind("submit", function(event) {
		$(this).trigger('calliSuccess');
		return true;
	});
	$(document).bind("calliError", showError);
});

function showError(event, error, detail) {
	setTimeout(function() {
		try {
			var e = jQuery.Event("error");
			if (typeof error == 'object') {
				jQuery.extend(true, e, error);
			} else {
				e.message = error;
			}
			if (detail && detail.indexOf('<') == 0) {
				var h1 = $(detail).find("h1").andSelf().filter("h1").clone();
				var frag = document.createDocumentFragment();
				h1.contents().each(function() {
					frag.appendChild(this);
				});
				e.message = frag;
				e.data = $(detail).find("pre").andSelf().filter("pre").text();
			}
			if (e.message) {
				var target = event.target;
				if (!target) {
					target = document;
				}
				$(target).trigger(e);
			}
		} catch (e) {
			alert(error);
		}
	}, 1000);
}

})(window.jQuery);

