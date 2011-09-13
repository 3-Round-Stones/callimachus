// auto-expand.js

(function($){

$(window).bind('resize', fillOutFlex);
$(document).bind('change', findAutoExpandTextArea);
$(document).bind('keypress', findAutoExpandTextArea);
$(document).bind('input', findAutoExpandTextArea);
$(document).bind('paste', findAutoExpandTextArea);
$(document).bind("DOMNodeInserted", findAutoExpandTextArea);
$(document).ready(function(){setTimeout(fillOutFlex, 0)});
$(window).load(function(event){
	$('iframe').load(fillOutFlex);
	$('img').load(fillOutFlex);
	fillOutFlex();
});

function fillOutFlex(){
	findFlex(document);
	findAutoExpandTextAreaIn(document);
}

function findFlex(target) {
	var areas = $(".flex", target);
	if ($(target).is(".flex")) {
		areas = areas.add(target);
	}
	areas.each(function() {
		flex(this);
	});
	var innerHeight = window.innerHeight || document.documentElement.clientHeight;
	if (innerHeight >= document.height) {
		// no scrollbars yet, assume they will appear
		setTimeout(function() {
			if (innerHeight < document.height) {
				findFlex(target);
			}
		}, 100);
	}
}

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

function flex(area) {
	var contentWidth = getAvailableWidth(area);
	var contentHeight = getAvailableHeight(area);
	if ($(area).is(":input")) {
		flexTextArea(area, contentWidth, contentHeight);
	} else if (area.nodeName.toLowerCase() == "iframe") {
		flexIframe(area, contentWidth, contentHeight);
	} else {
		flexBlock(area, contentWidth, contentHeight);
	}
}

function expand(area) {
	var contentWidth = getAvailableWidth(area);
	var contentHeight = getAvailableHeight(area);
	if ($(area).is(":input")) {
		expandTextArea(area, contentWidth, contentHeight);
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

function flexTextArea(area, contentWidth, innerHeight) {
	$(area).css('width', contentWidth);
	$(area).css('height', innerHeight);
}

function flexIframe(area, contentWidth, innerHeight) {
	if (area.contentWindow) {
		$(area).css('width', contentWidth);
		try {
			var height = 0;
			var body = $('body', area.contentWindow.document);
			body.children(":visible").each(function() {
				height += $(this).outerHeight(true);
			});
			height += parsePixel(body.css("padding-top"));
			height += parsePixel(body.css("padding-bottom"));
			height += parsePixel(body.css("border-top-width"));
			height += parsePixel(body.css("border-bottom-width"));
			height += parsePixel(body.css("margin-top"));
			height += parsePixel(body.css("margin-bottom"));
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

function flexBlock(area, contentWidth, innerHeight) {
	$(area).css('width', contentWidth);
	$(area).css('max-height', innerHeight);
	$(area).css('overflow', 'auto');
}

function getAvailableHeight(area) {
	var innerHeight = window.innerHeight || document.documentElement.clientHeight;
	var body = $(area).parents('body>*').offset().top + $(area).parents('body>*').outerHeight(true) - $(area).outerHeight(true);
	var formTop = $(area).parents('form').offset().top;
	var formHeight = $(area).parents('form').outerHeight(true) - $(area).outerHeight(true);
	var mainTop = $(area).parents('#content').offset().top;
	var mainHeight = $(area).parents('#content').outerHeight(true) - $(area).outerHeight(true);
	var contentHeight = innerHeight;
	if (body <= innerHeight / 3) {
		contentHeight -= body;
	} else if (mainHeight > 0 && mainTop > 0 && mainHeight + mainTop <= innerHeight / 3) {
		contentHeight -= mainHeight + mainTop;
	} else if (formHeight > 0 && formTop > 0 && formHeight + formTop <= innerHeight / 3) {
		contentHeight -= formHeight + formTop;
	} else if (formHeight > 0 && formHeight <= innerHeight / 3) {
		contentHeight -= formHeight;
	}
	return contentHeight;
}

function getAvailableWidth(area) {
	var margin = getMarginRight(area);
	var contentWidth = document.documentElement.clientWidth - $(area).offset().left - margin;
	var innerHeight = window.innerHeight || document.documentElement.clientHeight;
	if (innerHeight >= document.height) {
		// no scrollbars yet, assume they will appear
		contentWidth -= 32;
	}
	return contentWidth;
}

function getMarginRight(area) {
	var clientWidth = document.documentElement.clientWidth;
	var asideLeft = getAsideLeft(area);
	var parent = getParentBlock(area);
	var left = $(area).css("border-left-width");
	var right = $(area).css("border-right-width");
	var margin = $(area).css("margin-right-width");
	var marginRight = parsePixel(left) + parsePixel(right) + parsePixel(margin);
	var breakFlag = false;
	$(area).parents().each(function(){
		if (this == parent) {
			breakFlag = true;
		} else if (!breakFlag) {
			marginRight += parsePixel($(this).css("border-right-width"));
			marginRight += parsePixel($(this).css("margin-right"));
			marginRight += parsePixel($(this).css("padding-right"));
		}
	});
	marginRight += clientWidth - $(parent).offset().left - parsePixel($(parent).css('border-left-width')) - $(parent).width();
	if (marginRight >= clientWidth - asideLeft)
		return marginRight;
	return marginRight + clientWidth - asideLeft;
}

function getParentBlock(area) {
	var parent = null;
	$(area).parents().each(function(){
		if (!parent) {
			var display = $(this).css('display');
			var floatStyle = $(this).css('float');
			if (display == 'block' && (!floatStyle || floatStyle == 'none')) {
				parent = this;
			}
		}
	});
	if (parent)
		return parent;
	return $('body')[0];
}

function getAsideLeft(area) {
	var clientWidth = document.documentElement.clientWidth;
	var asideLeft = clientWidth;
	var areaTop = $(area).offset().top;
	$(".aside:visible").filter(function(){
		var top = $(this).offset().top;
		return areaTop < top + $(this).outerHeight(true) && areaTop + $(area).outerHeight(true) > top;
	}).each(function() {
		var left = $(this).offset().left;
		left -= parsePixel($(this).css("margin-left"));
		if (left < asideLeft) {
			asideLeft = left;
		}
	});
	return asideLeft;
}

function parsePixel(str) {
	if (!str)
		return 0;
	if (str.indexOf('px') > 0)
		return parseInt(str.substring(0, str.indexOf('px')));
	if (str == "thin")
		return 1;
	if (str == "medium")
		return 3;
	if (str == "thick")
		return 5;
	return 0;
}

})(window.jQuery);

