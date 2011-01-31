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
	var promptSpan = document.createElement("span");
	promptSpan.setAttribute("style", "position: absolute; font-style: italic; color: #aaa; margin: 0.2em 0 0 0.5em; cursor: text;");
	promptSpan.setAttribute('id', id + '-prompt');
	promptSpan.setAttribute("title", title);
	promptSpan.textContent = title;
	promptSpan.innerText = title;
	promptSpan.onmouseover = function() {
		promptSpan.style.display = "none";
	}
	promptSpan.onclick = function() {
		promptSpan.style.display = "none";
		input.focus();
	}
	if(input.value != '') {
		promptSpan.style.display = "none";
	}
	input.parentNode.insertBefore(promptSpan, input);
	input.onfocus = function() {
		promptSpan.style.display = "none";
	}
	input.onblur = function() {
		if(input.value == '') {
			promptSpan.style.display = "inline";
		}
	}
	input.onmouseout = function() {
		if(input.value == '' && input!=document.activeElement) {
			promptSpan.style.display = "inline";
		}
	}
}

})(window.jQuery);

