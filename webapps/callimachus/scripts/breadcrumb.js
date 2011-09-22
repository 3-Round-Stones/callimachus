// breadcrumb.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function select(node, selector) {
	var set = $(node).find(selector).andSelf();
	set = set.add($(node).parents(selector));
	return set.filter(selector);
}

function handle(event) {
	breadcrumb(select(event.target, ".breadcrumb"));
}

function breadcrumb(element) {
	element.children(':not(:first-child)').remove();
}

})(window.jQuery);

