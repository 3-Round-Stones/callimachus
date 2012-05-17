// dialog.js
/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

if (!window.calli) {
	window.calli = {};
}

window.calli.closeDialog = function(iframe) {
	var e = jQuery.Event("calliCloseDialog");
	var frameElement = findFrameElement(iframe);
	$(frameElement).trigger(e);
	if (!e.isDefaultPrevented()) {
		$(frameElement).dialog('close');
	}
}

function findFrameElement(iframe) {
	var elements = $('iframe').filter(function() {
		return iframe == this.contentWindow;
	});
	if (elements.length)
		return elements[0];
	return iframe.frameElement;
}

function asName(title) {
	if (!title)
		return "iframe";
	return title.toLowerCase().replace(/^\s+/,'').replace(/\s+$/,'').replace(/\s/g,'-').replace(/[^\-\w]/g,'_');
}

function asUniqueName(title) {
	var name = asName(title);
	if (!frames[name] || frames[name].name != name)
		return name;
	var i = 1;
	while (frames[name + i] && frames[name + i].name == name + i) {
		i++;
	}
	return name + i;
}

window.calli.openDialog = function(url, title, options) {
	/* var width = 450; */
	var height = 500;
	height += 50; // title bar
	if (options.buttons) {
		height += 50; // button bar
	}
	var innerHeight = window.innerHeight || document.documentElement.clientHeight;
	if (window.parent != window && parent.postMessage) {
		if (height > innerHeight) {
			parent.postMessage('PUT height\n\n' + height, '*');
		}
		if (width > document.documentElement.clientWidth) {
			parent.postMessage('PUT width\n\n' + width, '*');
		}
	}
	while (height > 200 && width > 200 && (height > innerHeight || width > document.documentElement.clientWidth)) {
		height -= 100;
		width -= 100;
	}
	var settings = jQuery.extend({
		title: title,
		autoOpen: false,
		modal: false,
		draggable: true,
		resizable: false,
		autoResize: true,
		position: ['center', 'center'],
		width: width,
		height: height
	}, options);
	var iframe = $("<iframe></iframe>");
	iframe.attr('src', 'about:blank');
	iframe.attr('name', asUniqueName(title));
	iframe.addClass('dialog');
	iframe.dialog(settings);
	var requestedHeight = height;
	var requestedWidth = width;
	var setDialogOuterHeight = function(outerHeight) {
		outerHeight = Math.min(outerHeight, window.innerHeight || document.documentElement.clientHeight);
		var height = outerHeight - iframe.parent().outerHeight(true) + iframe.parent().height();
		var fheight = height - iframe.outerHeight(true) + iframe.height();
		iframe.siblings().each(function(){
			if ($(this).css('position') != 'absolute') {
				fheight -= $(this).outerHeight(true);
			}
		});
		var previously = iframe.dialog("option", "height");
		iframe.dialog("option", "height", height);
		iframe.height(fheight);
		if (outerHeight - 50 > iframe.parent().outerWidth(true) && previously < height) {
			setDialogOuterWidth(outerHeight - 50);
		}
		iframe.dialog("option", "position", "center");
	};
	var setDialogOuterWidth = function(outerWidth) {
		outerWidth = Math.min(outerWidth, document.documentElement.clientWidth);
		var width = outerWidth - iframe.parent().outerWidth(true) + iframe.parent().width();
		var previously = iframe.dialog("option", "width");
		iframe.dialog("option", "width", width);
		if (outerWidth - 50 > iframe.parent().outerHeight(true) && previously < width) {
			setDialogOuterHeight(outerWidth - 50);
		}
		iframe.dialog("option", "position", "center");
	};
	var handle = function(event) {
		if (event.originalEvent.source == iframe[0].contentWindow) {
			var data = event.originalEvent.data;
			if (data.indexOf('PUT height\n\n') == 0) {
				var height = parseInt(data.substring(data.indexOf('\n\n') + 2));
				requestedHeight = height + iframe.parent().outerHeight(true) - iframe.height();
				var innerHeight = window.innerHeight || document.documentElement.clientHeight;
				if (requestedHeight <= innerHeight) {
					setDialogOuterHeight(requestedHeight);
				} else {
					setDialogOuterHeight(innerHeight);
					if (window.parent != window) {
						parent.postMessage('PUT height\n\n' + requestedHeight, '*');
					}
				}
				iframe[0].contentWindow.postMessage('OK\n\PUT height', '*');
			} else if (data.indexOf('PUT width\n\n') == 0) {
				var width = parseInt(data.substring(data.indexOf('\n\n') + 2));
				requestedWidth = width + iframe.parent().outerWidth(true) - iframe.width();
				if (requestedWidth <= document.documentElement.clientWidth) {
					setDialogOuterWidth(requestedWidth);
				} else {
					setDialogOuterWidth(document.documentElement.clientWidth);
					if (window.parent != window) {
						parent.postMessage('PUT width\n\n' + requestedWidth, '*');
					}
				}
				iframe[0].contentWindow.postMessage('OK\n\nPUT width', '*');
			} else if (typeof options.onmessage == 'function') {
				if (!event.source) {
					event.source = event.originalEvent.source;
				}
				if (!event.data) {
					event.data = event.originalEvent.data;
				}
				options.onmessage(event);
			}
		}
	};
	var onresize = function(){
		var clientWidth = document.documentElement.clientWidth;
		setDialogOuterWidth(Math.min(Math.max(450, requestedWidth), clientWidth));
		var innerHeight = window.innerHeight || document.documentElement.clientHeight;
		if (requestedWidth > clientWidth || requestedHeight > innerHeight) {
			iframe.dialog("option", "position", "center");
		}
		setDialogOuterHeight(Math.min(Math.max(500, requestedHeight), innerHeight));
	};
	$(window).bind('message', handle);
	$(window).bind('resize', onresize);
	iframe.one('load', function(){
		setTimeout(function() {
			iframe.dialog("option", "position", ['center', 'center']);
		}, 0);
	});
	iframe.bind("dialogclose", function(event, ui) {
		$(window).unbind('message', handle);
		$(window).unbind('resize', onresize);
		iframe.remove();
		iframe.parent().remove();
		if (typeof options.onclose == 'function') {
			options.onclose();
		}
	});
	var e = jQuery.Event("calliOpenDialog");
	iframe.trigger(e);
	if (e.isDefaultPrevented()) {
		iframe.trigger("dialogclose");
		return null;
	} else {
		iframe.dialog("open");
		iframe.css('width', '100%');
		iframe[0].src = url;
		if (typeof options.onlookup == 'function') {
			var dialogTitle = iframe.parents(".ui-dialog").find(".ui-dialog-title");
			var form = $("<form></form>");
			var searchTerms = $("<input/>");
			searchTerms.attr("placeholder", "Lookup..");
			form.append(searchTerms);
			form.css('position', "absolute");
			form.css('top', dialogTitle.offset().top - iframe.parent().offset().top - 5);
			form.css('right', 30);
			iframe.before(form);
			form.submit(function(event) {
				event.preventDefault();
				if (searchTerms.val()) {
					options.onlookup(searchTerms.val());
				}
				return false;
			});
		}
		var win = iframe[0].contentWindow;
		try {
			win.close = function() {
				window.setTimeout(function() {
					calli.closeDialog(win);
				}, 0);
			};
		} catch (e) {
			// use calli.closeDialog directly
		}
		return win;
	}
}

})(jQuery, jQuery);

