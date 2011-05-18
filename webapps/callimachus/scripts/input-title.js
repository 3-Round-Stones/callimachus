// input-title.js

(function($){

$(document).ready(handle);
$(document).bind("DOMNodeInserted", handle);

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function handle(event) {
	select(event.target, "input[title]").each(function(i, input) {
		var title = input.getAttribute("title");
		if (title) {
			initInputPromptTitle(input, title);
		}
	});
}

var counter = 0;

function initInputPromptTitle(input, title) {
	var id = input.id;
	if (!id) {
		id = 'input-' + (++counter);
	}
	var promptSpan = $("<span/>");
	promptSpan.attr("class", "watermark");
	promptSpan.attr("style", "position: absolute; margin: 0px 1ex; cursor: text; line-height: 1.75em; font-size: inherit");
	promptSpan.attr('id', id + '-prompt');
	promptSpan.attr("title", title);
	promptSpan.text(title);
	promptSpan.bind('mouseover', function() {
		promptSpan.css('display', "none");
	});
	promptSpan.bind('mouseout', function() {
		if(input.value == '' && input!=document.activeElement) {
			promptSpan.css('display', "inline");
		}
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
	};
	input.onblur = function() {
		if(input.value == '') {
			promptSpan.css('display', "inline");
		}
	};
	input.onmouseover = function() {
		promptSpan.css('display', "none");
	};
	input.onmouseout = function() {
		if(input.value == '' && input!=document.activeElement) {
			promptSpan.css('display', "inline");
		}
	};
}

})(window.jQuery);

