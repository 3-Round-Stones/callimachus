// error.js

(function($){

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
	forms.bind("calliError", function(event, status, detail, link) {
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
	});
});

})(window.jQuery);

