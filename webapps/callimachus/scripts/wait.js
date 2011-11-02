// wait.js

(function($){

document.documentElement.className += ' wait';
var requestCount = 1;
var lastWait = 0;

function removeWait() {
	$(function() {
		setTimeout(function() {
			requestCount--;
			if (requestCount < 1) {
				var myWait = ++lastWait;
				setTimeout(function() {
					if (myWait == lastWait && requestCount < 1) {
						requestCount = 0;
						$(document.documentElement).removeClass("wait");
					}
				}, 400); // give browser a chance to draw the page
			}
		}, 0);
	});
}

$(removeWait);

$(window).bind("beforeunload unload", function() {
	requestCount++;
	$(document.documentElement).addClass("wait");
});

$(document).ajaxSend(function(event, xhr, options){
	requestCount++;
	$(document.documentElement).addClass("wait");
});
$(document).ajaxComplete(removeWait);

})(window.jQuery);

