// auto-expand.js

(function($){

$(document).ready(function(event){setTimeout(function(){findAutoExpandTextArea(event)}, 0)});
$(window).bind('resize', function(){findAutoExpandTextAreaIn(document)});
$(document).bind('change', findAutoExpandTextArea);
$(document).bind('keypress', findAutoExpandTextArea);
$(document).bind('input', findAutoExpandTextArea);
$(document).bind('paste', findAutoExpandTextArea);
$(document).bind("DOMNodeInserted", findAutoExpandTextArea);

function findAutoExpandTextArea(event) {
	findAutoExpandTextAreaIn(event.target);
}

function findAutoExpandTextAreaIn(target) {
	var areas = $(".auto-expand", target);
	if ($(target).is(".auto-expand")) {
		areas = areas.add(target);
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
		left -= parsePixel($(this).css("margin-left"));
		if (left < asideLeft) {
			asideLeft = left;
		}
	});
	var left = $(area).css("border-left-width");
	var right = $(area).css("border-right-width");
	var marginRight = parsePixel(left) + parsePixel(right);
	$(area).parents().each(function(){
		marginRight += parsePixel($(this).css("border-right-width"));
		marginRight += parsePixel($(this).css("margin-right"));
		marginRight += parsePixel($(this).css("padding-right"));
	});
	var contentWidth = asideLeft - $(area).offset().left - marginRight;
	var innerHeight = window.innerHeight || document.documentElement.clientHeight;
	if (innerHeight >= document.height) {
		// no scrollbars yet, assume they will appear
		contentWidth -= 32;
		setTimeout(function() {
			if (innerHeight < document.height) {
				findAutoExpandTextAreaIn(document);
			}
		}, 100);
	}
	if (area.type == "textarea" ||  area.type == "text") {
		expandTextArea(area, contentWidth, innerHeight);
	} else if (area.nodeName.toLowerCase() == "iframe") {
		expandIframe(area, contentWidth, innerHeight);
	} else {
		expandBlock(area, contentWidth, innerHeight);
	}
}

function expandTextArea(area, contentWidth, innerHeight) {
	var width = area.cols || area.size;
	var height = area.rows;
	var maxCols = Math.min(Math.floor(contentWidth / area.offsetWidth * width - 3), Math.floor(contentWidth / 8 - 3));
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
	if (area.contentWindow) {
		$(area).css('width', contentWidth);
		try {
			var height = 0;
			var body = $('body', area.contentWindow.document);
			body.children(":visible").each(function() {
				height += $(this).height();
			});
			height += parsePixel(body.css("padding-top"));
			height += parsePixel(body.css("padding-bottom"));
			body.children(":visible").andSelf().each(function() {
				height += parsePixel($(this).css("border-top-width"));
				height += parsePixel($(this).css("border-bottom-width"));
				height += parsePixel($(this).css("margin-top"));
				height += parsePixel($(this).css("margin-bottom"));
			});
			height += parsePixel($(area).css("padding-top"));
			height += parsePixel($(area).css("padding-bottom"));
			height += parsePixel($(area).css("border-top-width"));
			height += parsePixel($(area).css("border-bottom-width"));
			if (height >= 100) {
				$(area).css('height', Math.min(innerHeight, height + 100));
				return;
			}
		} catch(e) {}
		$(area).css('height', innerHeight);
	}
}

function expandBlock(area, contentWidth, innerHeight) {
	var childHeight = 0;
	$(area).find(":visible").each(function() {
		var height = $(this).height();
		height += parsePixel($(this).css("border-top-width"));
		height += parsePixel($(this).css("border-bottom-width"));
		height += parsePixel($(this).css("margin-top"));
		height += parsePixel($(this).css("margin-bottom"));
		if (height > childHeight) {
			childHeight = height;
		}
	});
	$(area).css('width', contentWidth);
	$(area).css('height', Math.min(innerHeight, childHeight));
}

function parsePixel(str) {
	if (str && str.indexOf('px'))
		return parseInt(str.substring(0, str.indexOf('px')));
	return 0;
}

})(window.jQuery);

