// protected.js

(function($){

var user = '';

$(document).bind("calliLoggedOut", function() {
	user = '';
	hide($('.protected'));
});

$(document).bind("calliLoggedIn", function(event) {
	user = event.title;
	show($('.protected'), user);
});

$(document).bind("DOMNodeInserted", function handle(event) {
	if (user) {
		show(select(event.target, ".protected"), user);
	} else {
		hide(select(event.target, ".protected"));
	}
});

function select(node, selector) {
	return $(node).find(selector).add($(node).parents(selector)).andSelf().filter(selector);
}

function hide(set) {
	set.css('display', 'none');
}

function show(set, user) {
	set.css('display', '');
}

})(window.jQuery);

