// hbox.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);
$(window).resize(function(){hbox($('.hbox'))});

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function handle(event) {
	hbox(select(event.target, ".hbox"));
}

function hbox(element) {
	element.css("overflow", "hidden");
	var children = element.children();
	children.css("float", "left").css("margin-right", "1em").css("clear", "none");
	setTimeout(function() { // reposition children first
		children.css("clear", function() {
			if ($(this).prev().size() && $(this).position().top > $(this).prev().position().top) {
				return "left";
			} else {
				return "none";
			}
		});
	}, 0);
}

})(window.jQuery);

