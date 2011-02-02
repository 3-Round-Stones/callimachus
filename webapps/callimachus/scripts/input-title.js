// input-title.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
	$("input[title]", event.target).each(function(i, input) {
		var title = input.getAttribute("title");
		if (title) {
			initInputPromptTitle(input, title);
		}
	})
	if ($(event.target).is("input[title]")) {
		var title = event.target.getAttribute("title");
		if (title) {
			initInputPromptTitle(event.target, title);
		}
	}
}

var counter = 0;

function initInputPromptTitle(input, title) {
	var id = input.id;
	if (!id) {
		id = 'input-' + (++counter);
	}
	var promptSpan = $("<span/>");
	promptSpan.attr("style", "position: absolute; line-height:1.75em; font-size: smaller; font-style: italic; color: #aaa; margin: 0.2em 0 0 0.5em; cursor: text;");
	promptSpan.attr('id', id + '-prompt');
	promptSpan.attr("title", title);
	promptSpan.text(title);
	promptSpan.bind('mouseover', function() {
		promptSpan.css('display', "none");
	});
	promptSpan.bind('click', function() {
		promptSpan.css('display', "none");
		input.focus();
	});
	if(input.value != '') {
		promptSpan.css('display', "none");
	}
	input.parentNode.insertBefore(promptSpan[0], input);
	input.removeAttribute("title");
	input.onfocus = function() {
		promptSpan.css('display', "none");
	}
	input.onblur = function() {
		if(input.value == '') {
			promptSpan.css('display', "inline");
		}
	}
	input.onmouseout = function() {
		if(input.value == '' && input!=document.activeElement) {
			promptSpan.css('display', "inline");
		}
	}
}

})(window.jQuery);

