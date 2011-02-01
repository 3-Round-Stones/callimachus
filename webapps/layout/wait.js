// wait.js

(function($){

var formRequestCount = 1;
var lastWait = 0;
$(document).ready(function() {
	function removeWait() {
		formRequestCount--;
		if (formRequestCount < 1) {
			var myWait = ++lastWait;
			setTimeout(function() {
				if (myWait == lastWait && formRequestCount < 1) {
					formRequestCount = 0;
					$("body").removeClass("wait");
				}
			}, 500); // give browser a chance to draw the page
		}
	}
	setTimeout(removeWait, 0);
	$(window).unload(function (event) {
		$("body").addClass("wait");
		formRequestCount++;
	})
	$("body").ajaxSend(function(event, xhr, options){
		formRequestCount++;
		$("body").addClass("wait");
	})
	$("body").ajaxComplete(function(event, xhr, options){
		setTimeout(removeWait, 0);
	})
	var forms = $("form[about]");
	forms.bind("calliRedirect", function() {
		formRequestCount++;
		$("body").addClass("wait");
	})
	forms.bind("calliSubmit", function() {
		$("#error-widget").hide();
		$("#error-message").empty();
		$("#error-widget pre").remove();
	})
	forms.bind("calliOk", function() {
		$("#error-widget").hide();
		$("#error-message").empty();
		$("#error-widget pre").remove();
	})
	forms.bind("calliError", function(event, status, detail, link) {
		if (detail && detail.indexOf('<') == 0) {
			var html = $(detail);
			if (html.filter("title").length) {
				status = html.filter("title").text();
				detail = html.find("pre").text();
			}
		}
		var msg = $("#error-message");
		if (msg.size()) {
			msg.empty()
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
			msg.focus();
			if (detail) {
				var pre = $("<pre/>");
				pre.text(detail);
				pre.hide();
				$("#error-widget").append(pre)
				msg.click(function() {
					pre.toggle();
				})
			}
		} else {
			alert(status);
		}
	})
})

})(window.jQuery);

