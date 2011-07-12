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
	set.each(function() {
		var node = $(this);
		var groups = node.attr("data-group");
		if (groups) {
			$(groups.split(/\s+/)).each(function() {
				var group = this;
				ifHasMembership(user, group, function() {
					node.css('display', '');
				}, function() {
					node.css('display', 'none');
				});
			});
		} else {
			node.css('display', '');
		}
	});
}

function ifHasMembership(user, group, member, nonmember) {
	jQuery.get(group + '?membership', function(text) {
		if ("true" == text) {
			member();
		} else {
			nonmember();
		}
	});
}

})(window.jQuery);

