// wait.js

(function($){

document.documentElement.className += ' wait';
var formRequestCount = 1;
var lastWait = 0;

$(window).bind("beforeunload unload", function() {
	formRequestCount++;
	$(document.documentElement).addClass("wait");
});

$(document).ajaxSend(function(event, xhr, options){
	formRequestCount++;
	$(document.documentElement).addClass("wait");
});

$(document).ajaxComplete(function(event, xhr, options){
	$(function() {
		setTimeout(removeWait, 0);
	});
});

$(function() {
	setTimeout(removeWait, 0);
});

function removeWait() {
	formRequestCount--;
	if (formRequestCount < 1) {
		var myWait = ++lastWait;
		setTimeout(function() {
			if (myWait == lastWait && formRequestCount < 1) {
				formRequestCount = 0;
				$(document.documentElement).removeClass("wait");
			}
		}, 500); // give browser a chance to draw the page
	}
}

})(window.jQuery);

