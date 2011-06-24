// data-options.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	loadOptions($("[data-options]"));
});

$(document).bind('DOMNodeInserted', function (event) {
	loadOptions($(event.target).find("[data-options]").andSelf().filter("[data-options]"));
});

function loadOptions(selects) {
	selects.each(function() {
		var select = $(this);
		var url = select.attr("data-options");
		select.removeAttr("data-options");
		jQuery.get(url, function(data) {
			var options = $(data);
			var selected = select.children('option,label');
			options.contents().each(function() {
				if (this.nodeType == 3) {
					select.append(this); // text node
				} else if (this.nodeType == 1 && $(this).is('option,label')) {
					var option = $(this);
					var checked = filterByAttributes(selected, option);
					var bool = checked.is('option:selected,label:has(input:checked)');
					if (!bool) {
						disableRDFa(option);
						option.removeAttr("selected");
						option.children('input').removeAttr("checked");
					}
					checked.remove();
					select.append(option);
					if (bool && option.is('option')) {
						// drop down auto selects first, until another is progamically selected
						select[0].options[select[0].options.length - 1].selected = selected;
					}
				}
			});
			select.find('input:radio').each(function() {
				this.checked = this.getAttribute('checked') != null;
			});
			var controls = select.parents().andSelf().filter('select').add(select.find('input'));
			controls.change(function() {
				select.children("option,label").each(function(){
					var option = $(this);
					if (option.is('option:selected') || option.children('input:checked').length) {
						enableRDFa(option);
					} else {
						disableRDFa(option);
					}
				});
			});
		});
	});
}

function filterByAttributes(set, node) {
	return set.filter(function() {
		return 0 == countDifferentAttributes($(this), node);
	});
}

var RDFATTR = ["about", "typeof", "rel", "rev", "resource", "property", "href", "src"];

function countDifferentAttributes(selected, option) {
	var count = 0;
	for (var i = 0; i < RDFATTR.length; i++) {
		if (selected.attr(RDFATTR[i]) != option.attr(RDFATTR[i])) {
			count++;
		}
	}
	return count;
}

function disableRDFa(element) {
	element.find('*').andSelf().each(function() {
		var node = $(this);
		for (var i = 0; i < RDFATTR.length; i++) {
			disableAttribute(node, RDFATTR[i]);
		}
	});
}

function enableRDFa(element) {
	element.find('*').andSelf().each(function() {
		var node = $(this);
		for (var i = 0; i < RDFATTR.length; i++) {
			enableAttribute(node, RDFATTR[i]);
		}
	});
}

function disableAttribute(node, attr) {
	if (node.attr(attr)) {
		node.attr('data-' + attr, node.attr(attr));
		node.removeAttr(attr);
	}
}

function enableAttribute(node, attr) {
	if (node.attr('data-' + attr)) {
		node.attr(attr, node.attr('data-' + attr));
		node.removeAttr('data-' + attr);
	}
}

})(jQuery);

