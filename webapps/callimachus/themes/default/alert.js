// alert.js

(function($){

$(document).error(function(event) {
	var status = event.message;
	var detail = event.data;
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
});

$(document).bind("calliSuccess", function() {
	$("#error-widget").hide();
	$("#error-message").empty();
	$("#error-widget pre").remove();
});

})(window.jQuery);

