// focus.js

jQuery(function($){
	$(document).bind("calliReady", function() {
		$(".iframe #content :input:visible:not(button):first").focus();
	});
});
