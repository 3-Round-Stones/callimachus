// error.js

(function($){

$(document).ajaxError(function(event, xhr, ajaxOptions, thrownError){
	if (xhr && xhr.status >= 400 && xhr.status != 404 && xhr.status != 409) {
		showError(event, xhr.statusText, xhr.responseText)
	} else if (thrownError) {
		showError(event, thrownError);
	}
});

$(document).ready(function() {
	var forms = $("form[about]");
	forms.bind("calliSubmit", function() {
		$("#error-widget").hide();
		$("#error-message").empty();
		$("#error-widget pre").remove();
	});
	forms.bind("calliOk", function() {
		$("#error-widget").hide();
		$("#error-message").empty();
		$("#error-widget pre").remove();
	});
	forms.bind("calliError", showError);
});

function showError(event, status, detail, link) {
	if (detail && detail.indexOf('<') == 0) {
		var html = $(detail);
		if (html.filter("title").size()) {
			status = html.filter("title").text();
			detail = html.find("pre").text();
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
}

})(window.jQuery);

