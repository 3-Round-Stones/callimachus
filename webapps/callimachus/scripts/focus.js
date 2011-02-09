// focus.js

jQuery(function($){
	$("form").bind("calliForm", function() {
		$(this).find(":input:visible:first").focus();
	});
});
