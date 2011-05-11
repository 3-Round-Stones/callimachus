// data-options.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	var form = $("form[about]");
	initInputRelElements(form);
	initOptionElements(form);
});

$("form[about]").live("DOMNodeInserted", function (event) {
	initInputRelElements(event.target);
	initOptionElements(event.target);
});

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function initInputRelElements(form) {
	select(form, "[data-options]:not(:input,optgroup)").each(function(i, node) {
		var script = $(node);
		var objects = script.children("[resource]")
		loadOptions(script, objects, "checked", function() {
			script.append($(this));
		})
	})
}

function initOptionElements(form) {
	select(form, "select").each(function(i, node) {
		var select = $(node);
		var script = select.is("[data-options]") ? select : select.children("[data-options]");
		var rel = script.attr("data-rel");
		if (script.size()) {
			select.change(function(){
				$("option:selected", this).each(function(i, option){
					var $option = $(option);
					$option.removeAttr("about");
					$option.attr("rel", rel);
					$option.attr("resource", $option.attr("data-resource"));
				});
				$("option:not(:selected)", this).each(function(i, option){
					var $option = $(option);
					$option.removeAttr("rel");
					$option.removeAttr("resource");
					$option.attr("about", $option.attr("data-resource"));
				});
			});
			var objects = $("[resource]", select);
			script.each(function() {
				var optgroup = $(this);
				loadOptions(optgroup, objects, "selected", function(i) {
					var option = $(this);
					optgroup.append(option);
					option.attr("data-resource", option.attr("resource"));
					if (!option.attr("rel") && option.attr("resource")) {
						option.removeAttr("resource");
						option.attr("about", option.attr("data-resource"));
					}
				}, function() {
					if (objects.size() == 1) {
						for (var i=0; i<node.options.length; i++) {
							if (node.options[i].getAttribute("rel")) {
								node.selectedIndex = i;
							}
						}
					}
					select.change();
				});
			});
		}
	});
}

function loadOptions(script, objects, selected, callback, refresh) {
	jQuery.get(script.attr("data-options"), function(data) {
		var options = $(data).children();
		var rel = script.attr("data-rel");
		var inputs = $("input", options);
		options.removeAttr("rel");
		if (selected) {
			options.removeAttr(selected);
			inputs.removeAttr(selected);
		}
		if (objects.size() && selected) {
			objects.each(function(i, obj){
				var uri = $(obj).attr("resource");
				$(options.get()).filter("[resource='" + uri + "']").each(function() {
					var option = $(this);
					if (inputs.size()) {
						$("input", option).attr(selected, selected);
					} else {
						option.attr(selected, selected);
					}
					option.attr("rel", rel);
					$(obj).remove();
				});
			});
		}
		if (inputs.size()) {
			options.each(function() {
				var option = $(this);
				option.attr("data-resource", option.attr("resource"));
				if (!option.attr("rel") && option.attr("resource")) {
					option.removeAttr("resource");
					option.attr("about", option.attr("data-resource"));
				}
				$("input", option).change(function(){
					if ($(this).is(':checked') && !option.attr("rel")) {
						option.removeAttr("about");
						option.attr("rel", rel);
						option.attr("resource", option.attr("data-resource"));
						$("[resource] input", script).change();
					} else if (!$(this).is(':checked') && option.attr("rel")) {
						option.attr("about", option.attr("data-resource"));
						option.removeAttr("rel");
						option.removeAttr("resource");
						$("[resource] input", script).change();
					}
				});
			});
		}
		options.each(function(i, node) {
			callback.call(this, i, node);
		})
		if (refresh) {
			setTimeout(refresh, 500);
		}
	});
}

})(jQuery);

