// auto-expand.js

(function($){

$(document).ready(function(event){setTimeout(function(){findAutoExpandTextArea(event)}, 0)});
$(window).bind('resize', findAutoExpandTextArea);
$(document).bind('change', findAutoExpandTextArea);
$(document).bind('keypress', findAutoExpandTextArea);
$(document).bind('input', findAutoExpandTextArea);
$(document).bind('paste', findAutoExpandTextArea);
$(document).bind("DOMNodeInserted", findAutoExpandTextArea);

function findAutoExpandTextArea(event) {
	var areas = $(".auto-expand", event.target);
	if ($(event.target).is(".auto-expand")) {
		areas = areas.add(event.target);
	}
	for (var i = 0; i < areas.length; i++) {
		if (areas[i].type == "textarea" || areas[i].type == "text") {
			expandTextArea(areas[i]);
		}
	}
	$(areas).unbind("paste", targetAutoExpandTextArea);
	$(areas).bind("paste", targetAutoExpandTextArea);
}

function targetAutoExpandTextArea(event) {
	var target = null;
	if (event.target && event.target.type == "textarea") {
		target = event.target;
	} else if (event.srcElement && event.srcElement.type == "textarea") {
		target = event.srcElement;
	} else if (event.target && event.target.type == "text") {
		target = event.target;
	} else if (event.srcElement && event.srcElement.type == "text") {
		target = event.srcElement;
	}
	if (target && target.className.match(/\bauto-expand\b/)) {
		setTimeout(function(){expandTextArea(target)}, 0);
	}
}

function expandTextArea(area) {
	var asideLeft = document.documentElement.clientWidth;
	$(".aside").filter(function(){
		var top = $(this).offset().top;
		var parent = $(area).offsetParent();
		return parent.offset().top < top + $(this).height() && parent.offset().top + parent.height() > top;
	}).each(function() {
		if ($(this).offset().left < asideLeft) {
			asideLeft = $(this).offset().left;
		}
	});
	var contentWidth = asideLeft - $(area).offset().left;
	var innerHeight = window.innerHeight || document.documentElement.clientHeight;
	var width = area.cols || area.size;
	var height = area.rows;
	var maxCols = Math.floor(contentWidth / area.offsetWidth * width - 3);
	var maxRows = Math.floor(innerHeight / area.offsetHeight * height - 3);
	if (!maxCols || maxCols < 2) {
		maxCols = 96;
	}
	if (!maxRows || maxRows < 2) {
		maxRows = 43;
	}
	var lines = area.value.split("\n");
	var cols = 20;
	var rows = Math.max(1, lines.length);
	for (var i = 0; i < lines.length; i++) {
		var len = lines[i].replace(/\t/g, "        ").length;
		if (cols < len + 1) {
			cols = len + 1;
		}
		rows += Math.floor(len / maxCols);
	}
	if (area.type == "textarea") {
		area.cols = Math.min(maxCols, cols);
		area.rows = Math.min(maxRows, rows + 1);
	} else {
		area.size = Math.min(maxCols, cols);
	}
}

})(window.jQuery);

