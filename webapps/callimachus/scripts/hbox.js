// hbox.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);
$(window).resize(function(){hbox($('.hbox'))});

function select(node, selector) {
	var set = $(node).find(selector).andSelf();
	set = set.add($(node).parents(selector));
	return set.filter(selector);
}

function handle(event) {
	hbox(select(event.target, ".hbox"));
}

function hbox(element) {
	element.css("overflow", "hidden");
	var children = element.children();
	children.css("float", "left").css("clear", "none");
	var siblings = children.filter(':not(:first-child)');
	siblings.css("margin-left", "1em");
	setTimeout(function() { // reposition children first
		siblings.css("clear", function() {
			if ($(this).prev().size() && $(this).position().top > $(this).prev().position().top) {
				$(this).css("margin-left", "0px");
				return "left";
			} else {
				$(this).css("margin-left", "1em");
				return "none";
			}
		});
	}, 0);
}

})(window.jQuery);

