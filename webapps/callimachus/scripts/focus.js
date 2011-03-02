// focus.js

jQuery(function($){
	$(document).bind("calliReady", function() {
		$("#content :input:visible:first").focus();
	});
});
