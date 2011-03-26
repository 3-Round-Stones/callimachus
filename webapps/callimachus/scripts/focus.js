// focus.js

jQuery(function($){
	$(document).bind("calliReady", function() {
		$(".iframe #content :input:visible:first").focus();
	});
});
