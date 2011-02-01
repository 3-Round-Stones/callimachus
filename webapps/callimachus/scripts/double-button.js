// double-button.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInsertedIntoDocument", handle);

function handle(event) {
	$("button", event.target).click(flashButton);
	if ($(event.target).is("button")) {
		$(event.target).click(flashButton);
	}
}

function flashButton(event) {
	var button = $(event.target);
	setTimeout(function() {
		if (!button.attr('disabled')) {
			button.attr('disabled', 'disabled');
			setTimeout(function() {
				button.removeAttr('disabled');
			}, 5000);
		}
	}, 0); // yield
	return true;
}

})(window.jQuery);
