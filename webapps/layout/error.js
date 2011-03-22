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
	} else if (xhr && xhr.status < 100 && !unloading) {
		showError(event, "Could not connect to server, please try again later");
	}
});

$(document).ajaxSuccess(function(event, xhr, ajaxOptions){
	if (xhr && xhr.status >= 200 && xhr.status < 300) {
		$("#error-widget").hide();
		$("#error-message").empty();
		$("#error-widget pre").remove();
	}
});

$(document).ready(function() {
	$(document).bind("calliSubmit", function() {
		$("#error-widget").hide();
		$("#error-message").empty();
		$("#error-widget pre").remove();
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
		var msg = $("#error-message");
		if (msg.size()) {
			msg.empty();
			msg.append(status);
			$("#error-widget pre").remove();
			$("#error-widget").show();
			scroll(0,0);
			if (detail) {
				var pre = $("<pre/>");
				pre.text(detail);
				pre.hide();
				$("#error-widget").append(pre);
				msg.click(function() {
					pre.toggle();
				});
			}
		} else {
			alert(status);
		}
	} catch (e) {
		alert(error);
	}
	}, 0);
}

})(window.jQuery);

