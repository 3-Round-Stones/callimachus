// error.js

(function($){

$(document).ajaxError(function(event, xhr, ajaxOptions, thrownError){
	if (xhr && xhr.status >= 400 && xhr.status != 409) {
		showError(event, xhr.statusText, xhr.responseText);
	} else if (thrownError) {
		showError(event, thrownError);
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
	$(document).bind("calliOk", function() {
		$("#error-widget").hide();
		$("#error-message").empty();
		$("#error-widget pre").remove();
	});
	$(document).bind("calliError", showError);
});

function showError(event, error, detail, link) {
	setTimeout(function() {
	try {
		var status = '' + error;
		if (detail && detail.indexOf('<') == 0) {
			var title = /<(?:\w*:)?title[^>]*>([^<]*)<\/(?:\w*:)?title>/i.exec(detail);
			if (title && title[1]) {
				status = title[1];
				detail = $(detail).find("pre").text();
			}
		}
		var msg = $("#error-message");
		if (msg.size()) {
			msg.empty();
			if (link) {
				if (status.indexOf('.') < 0) {
					msg.text(status + '.');
				}
				msg.append("<a href='" + link + "' style='font-style:italic;text-decoration:underline;margin-left:1ex'>View resource</a>");
			} else {
				msg.text(status);
			}
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

