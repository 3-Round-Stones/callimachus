// auto-expand.js

(function($){

$(document).ready(function(event){setTimeout(function(){findAutoExpandTextArea(event)}, 0)});
$(window).bind('resize', function(){findAutoExpandTextArea(document)});
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
		expand(areas[i]);
	}
	$(areas).unbind("paste", targetAutoExpandTextArea);
	$(areas).bind("paste", targetAutoExpandTextArea);
}

function targetAutoExpandTextArea(event) {
	if (event.target && event.target.className.match(/\bauto-expand\b/)) {
		setTimeout(function(){
			expand(event.target);
		}, 0);
	}
}

function expand(area) {
	var clientWidth = document.documentElement.clientWidth;
	var asideLeft = clientWidth;
	$(".aside:visible").filter(function(){
		var top = $(this).offset().top;
		var parent = $(area).offsetParent();
		return parent.offset().top < top + $(this).height() && parent.offset().top + parent.height() > top;
	}).each(function() {
		var left = $(this).offset().left;
		left -= $(this).css("margin-left").replace(/px/,'');
		if (left < asideLeft) {
			asideLeft = left;
		}
	});
	var left = $(area).css("border-left-width");
	var right = $(area).css("border-right-width");
	var marginRight = parseInt(left.replace(/px/,'')) + parseInt(right.replace(/px/,''));
	$(area).parents().each(function(){
		marginRight += parseInt($(this).css("border-right-width").replace(/px/,''));
		marginRight += parseInt($(this).css("margin-right").replace(/px/,''));
		marginRight += parseInt($(this).css("padding-right").replace(/px/,''));
	});
	var contentWidth = asideLeft - $(area).offset().left - marginRight;
	var innerHeight = window.innerHeight || document.documentElement.clientHeight;
	if (innerHeight > document.height) {
		// no scrollbars yet, assume they will appear
		contentWidth -= 32;
	}
	if (area.type == "textarea" ||  area.type == "text") {
		expandTextArea(area, contentWidth, innerHeight);
	} else if (area.nodeName.toLowerCase() == "iframe") {
		expandIframe(area, contentWidth, innerHeight);
	} else {
		expandBlock(area, contentWidth, innerHeight);
	}
	setTimeout(function() {
		if (clientWidth > document.documentElement.clientWidth) {
			// the resize might have caused scrollbar to appear
			expand(area);
		}
	}, 100);
}

function expandTextArea(area, contentWidth, innerHeight) {
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

function expandIframe(area, contentWidth, innerHeight) {
	$(area).css('width', contentWidth);
	try {
		var height = area.contentWindow.document.height;
		if (height > 100) {
			$(area).css('height', Math.min(innerHeight, height));
			return;
		}
	} catch(e) {}
	$(area).css('height', innerHeight);
}

function expandBlock(area, contentWidth, innerHeight) {
	var childHeight = 0;
	$(area).find(":visible").each(function() {
		var height = $(this).height();
		height += parseInt($(this).css("border-top-width").replace(/px/,''));
		height += parseInt($(this).css("border-bottom-width").replace(/px/,''));
		height += parseInt($(this).css("margin-top").replace(/px/,''));
		height += parseInt($(this).css("margin-bottom").replace(/px/,''));
		if (height > childHeight) {
			childHeight = height;
		}
	});
	$(area).css('width', contentWidth);
	$(area).css('height', Math.min(innerHeight, childHeight));
}

})(window.jQuery);

