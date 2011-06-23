// optional.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function select(node, selector) {
	return $(node).find(selector).add($(node).parents(selector)).andSelf().filter(selector);
}

function handle(event) {
	select(event.target, ".optional").each(function() {
		var node = $(this);
		if (node.find("[about],[src],[typeof],[typeof=''],[resource],[href],[property]").length) {
			node.show();
		} else {
			node.hide();
		}
	});
}

})(window.jQuery);

