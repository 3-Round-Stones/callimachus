// menu-edit.js

jQuery(function($){
	$('span[rel]').each(function() {
		$(this).text($(this).attr('resource'));
	});
	$('#items>li').each(function(p,n){parseItem(p,n,true)});
	$('#form').bind('cut paste', function() {
		setTimeout(function() {$('#items>li').each(function(p,n) {parseItem(p,n,true)});}, 0);
		return true;
	});
	$('#form *').blur(function() {
		$('#items>li').each(parseItem);
	});

	$('#form').submit(function() {
		$('#items>li').each(function(p,n){parseItem(p,n,true)});
		return true;
	});

	$('#form *').live('keydown', function(event) {
		var focus = getFocusNode();
		if (focus) {
			var li = $(focus);
			if (!li.is("li")) {
				li = li.parents("li:first");
			}
			if (event.keyCode == 8 && !textContent(li)) { // BACKSPACE
				removeItem(li);
				event.preventDefault();
				return false;
			} else if (event.keyCode == 9 && event.shiftKey) { // shift-TAB
				unindent(li);
				event.preventDefault();
				return false;
			} else if (event.keyCode == 9) { // TAB
				indent(li);
				event.preventDefault();
				return false;
			}
		}
		return true;
	});

	function getFocusNode() {
		if (window.getSelection)
			return window.getSelection().focusNode;
		return document.selection.createRange().parentElement();
	}

	function parseItem(position, node, trim) {
		var item = $(node);
		var span = item.children("span[property]");
		var label = item.children("label");
		var link = item.children("span[rel]");
		var ol = item.children("ol");
		ol.children("li").each(function(p, n) { parseItem(p, n, trim); });
		item.find("br").remove();
		if (!item.attr("about") && label.text()) {
			item.attr("about", $('body').attr("about") + "#" + label.text().toLowerCase().replace(/\W/, ''));
		} else {
			var about = item.attr("about");
			if ($('li[about="' + about + '"]').length > 1) {
				item.attr("about", $('body').attr("about") + "#" + label.text().toLowerCase().replace(/\W/, ''));
			}
		}
		if (!link.text()) {
			link.remove();
		}
		var text = textContent(node);
		if (!text && (trim || ol.children("li").length)) {
			removeItem(item);
		} else if (text) {
			var url = '';
			var m = text.match(/^(.*)\s+(\w+:\S+)$/);
			if (m) {
				text = m[1];
				url = m[2];
			} else if (item.find("a").size()) {
				url = item.find("a")[0].href;
			}
			if (span.text() != '' + position || label.text() != text || link.attr("resource") != url) {
				setLabelLink(item, position, text, url);
				item.attr("about", $('body').attr("about") + "#" + text.toLowerCase().replace(/\W/, ''));
			}
		}
		if (!ol.children("li").size()) {
			ol.remove();
		}
	}

	function setLabelLink(item, position, text, url) {
		var span = item.children("span[property]");
		var label = item.children("label");
		var link = item.children("span[rel]");
		var ol = item.children("ol");
		var trail = item.children("ol~button");
		if (!span.size()) {
			span = $("<span/>");
		}
		span.attr('property', 'calli:position');
		span.attr('datatype', 'xsd:integer');
		span.attr('content', position);
		if (!label.size()) {
			label = $("<label/>");
		}
		label.attr('property', 'rdfs:label');
		label.text(text);
		if (url) {
			if (!link.size()) {
				link = $("<span/>");
			}
			link.attr('rel', 'calli:link');
			link.attr('resource', url);
			link.text(url);
		} else {
			link.remove();
		}
		item.empty();
		item.append(span);
		item.append(label);
		if (url) {
			item.append(" ");
			item.append(link);
		}
		if (ol.children("li").size()) {
			item.append(ol);
		}
		item.append(trail);
	}

	function removeItem(li) {
		var prev = li.prev();
		var list = li.parent();
		var parent = list.parent("li");
		var children = li.children("ol").children("li");
		if (!li.siblings("li").size()) {
			list.remove();
		} else {
			li.remove();
		}
		if (prev.children("ol").size() && children.size()) {
			prev.children("ol").append(children);
		} else if (prev.size() && children.size()) {
			var ol = $("<ol/>");
			ol.attr('rel', 'calli:item');
			ol.append(children);
			prev.append(ol);
		} else if (list.size() && children.size()) {
			list.prepend(children);
		}
		if (document.createRange) {
			var sel = getSelection();
			if (prev.size()) {
				var range = document.createRange();
				range.setStartAfter(prev[0]);
				sel.removeAllRanges();
				sel.addRange(range);
			} else if (parent.size()) {
				var range = document.createRange();
				range.setStartAfter(parent[0]);
				sel.removeAllRanges();
			}
		}
	}

	function indent(li) {
		if (li.prev().size()) {
			var ol = li.prev().children("ol");
			if (!ol.size()) {
				ol = $("<ol/>");
				ol.attr('rel', 'calli:item');
				li.prev().append(ol);
			}
			li.remove();
			ol.append(li);
			if (document.createRange) {
				var range = document.createRange();
				range.setStartBefore(li[0]);
				var sel = getSelection();
				sel.removeAllRanges();
				sel.addRange(range);
			}
		}
	}

	function unindent(li) {
		if (li.parent("ol").parent("li").size()) {
			var parent = li.parent("ol").parent("li");
			li.remove();
			parent.after(li);
			if (document.createRange) {
				var range = document.createRange();
				range.setStartBefore(li[0]);
				var sel = getSelection();
				sel.removeAllRanges();
				sel.addRange(range);
			}
		}
	}

	function textContent(item) {
		var text = [];
		var nodes = $(item)[0].childNodes;
		for (var i = 0; i<nodes.length; i++) {
			var node = nodes[i];
			if (node.nodeName == "OL")
				break;
			if (node.nodeType == 3) { // TEXT
				text[text.length] = node.data + ' ';
			} else if (node.nodeType == 1 && $(node).is(":visible")) { // ELEMENT
				text[text.length] = $(node).text() + ' ';
			}
		}
		var m = text.join('').replace(/\s+/g, ' ').match(/^\s*(.*\S)\s*$/);
		if (m)
			return m[1];
		return null;
	}
});
