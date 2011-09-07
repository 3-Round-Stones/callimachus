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
	$(document).bind("calliSubmit", function() {
		$(document).trigger('calliSuccess');
	});
	$(document).bind("calliError", showError);
});

function showError(event, error, detail) {
	setTimeout(function() {
		try {
			var status = '' + error;
			if (detail && detail.indexOf('<') == 0) {
				status = $(detail).find("h1").andSelf().filter("h1").html();
				detail = $(detail).find("pre").andSelf().filter("pre").text();
			}
			var target = event.target;
			if (!target) {
				target = document;
			}
			var e = jQuery.Event("error");
			e.message = status;
			e.data = detail;
			$(target).trigger(e);
		} catch (e) {
			alert(error);
		}
	}, 1000);
}

})(window.jQuery);

