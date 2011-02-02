// wait.js

(function($){

var formRequestCount = 1;
var lastWait = 0;

$(window).unload(function (event) {
	$("body").addClass("wait");
	formRequestCount++;
});

$(document).ajaxSend(function(event, xhr, options){
	formRequestCount++;
	$("body").addClass("wait");
});

$(document).ajaxComplete(function(event, xhr, options){
	setTimeout(removeWait, 0);
});

$(document).ready(function() {
	setTimeout(removeWait, 0);
	$("form[about]").bind("calliRedirect", function() {
		formRequestCount++;
		$("body").addClass("wait");
	});
});

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

})(window.jQuery);

