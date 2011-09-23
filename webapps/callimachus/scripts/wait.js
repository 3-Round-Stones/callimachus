// wait.js

(function($){

document.documentElement.className += ' wait';
var requestCount = 1;
var lastWait = 0;

if (window.parent != window && window.parent.postMessage) {
	parent.postMessage('POST wait\n\ntrue', '*');
}

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
						if (window.parent != window && window.parent.postMessage) {
							parent.postMessage('POST wait\n\nfalse', '*');
						}
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

$(window).bind('message', function(event) {
	var data = event.originalEvent.data;
	var source = event.originalEvent.source;
	if (data.indexOf('POST wait\n\n') == 0) {
		$('iframe').each(function(){
			if (this.contentWindow == source) {
				var bool = data.substring(data.indexOf('\n\n') + 2);
				if (bool == 'true') {
					requestCount++;
					$(document.documentElement).addClass("wait");
				} else {
					removeWait();
				}
			}
		});
	}
});

})(window.jQuery);

