// input-expression.js
/*
   Copyright (c) 2011 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($){

$(document).ready(function () {
	bindInputChange($("input[data-expression-value]"));
	bindTextAreaChange($("textarea[data-text-expression]"));
});

$(document).bind("DOMNodeInserted", function (event) {
	bindInputChange(select(event.target, "input[data-expression-value]"));
	bindTextAreaChange(select(event.target, "textarea[data-text-expression]"));
});

function select(node, selector) {
	return $(node).find(selector).andSelf().filter(selector);
}

function bindInputChange(inputs) {
	inputs.change(function(event) {
		var input = $(this);
		var expression = input.attr("data-expression-value");
		propagate(input, expression, input.val());
	});
	inputs.change();
}

function bindTextAreaChange(areas) {
	areas.change(function(event) {
		var area = $(this);
		var expression = area.attr("data-text-expression");
		propagate(area, expression, area.val());
	});
	areas.change();
}

function propagate(node, expression, value) {
	var declaration = filter(node.parents().andSelf(), expression);
	if (declaration.length) {
		if (value) {
			enableRDFa(declaration);
			subsitute(expression, value, declaration);
		} else {
			disableRDFa(declaration);
		}
	} else if (expression.indexOf(':') > 0) {
		if (value) {
			enableRDFa(node);
			node.attr("property", expression);
			node.attr("content", value);
		} else {
			disableRDFa(node);
		}
	}
}

function filter(set, expression) {
	return set.filter(function() {
		var attrs = this.attributes;
		for (var i = 0; i < attrs.length; i++) {
			if (attrs[i].name.indexOf('data-var-') == 0 && attrs[i].value == expression) {
				return true;
			}
		}
	});
}

function subsitute(expression, value, set) {
	set.each(function() {
		var attrs = this.attributes;
		for (var i = 0; i < attrs.length; i++) {
			if (attrs[i].name.indexOf('data-var-') == 0 && attrs[i].value == expression) {
				this.setAttribute(attrs[i].name.replace(/data-var-/, ''), value);
			}
		}
	});
}

var RDFATTR = ["about", "typeof", "rel", "rev", "resource", "property", "href", "src"];

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

