// page-edit-wym.js

jQuery(function($){

	function outter(node) {
		var html = $($('<div/>').html($(node).clone())).html();
		$(node).remove();
		return html;
	}

	$(document).ready(function() {
		WYMeditor.XhtmlValidator._attributes['core']['attributes'].push('data-cardinality', 'data-prompt');
		WYMeditor.XhtmlValidator._attributes['core']['attributes'].push(
			'rel',
			'rev',
			'content',
			'href',
			'src',
			'about',
			'property',
			'resource',
			'datatype',
			'typeof');
		$('.wym_box_0').wymeditor({
			html: '<h1></h1>',
			lang: null,
			initSkin: false,
			loadSkin: false,
			jQueryPath: '/callimachus/scripts/jquery.js',
			dialogFeatures: 'jQuery.dialog',
			dialogFeaturesPreview: 'jQuery.dialog',
			dialogImageHtml: outter($('.wym_dialog_image')),
			dialogTableHtml: outter($('.wym_dialog_table')),
			dialogPasteHtml: outter($('.wym_dialog_paste')),
			dialogPreviewHtml: outter($('.wym_dialog_preview')),
			dialogLinkHtml: outter($('.wym_dialog_link'))
		});
	});

	function select(node, selector) {
		var set = $(node).find(selector).andSelf();
		set = set.add($(node).parents(selector));
		return set.filter(selector);
	}

	function setFocusToNode(wym, node) {
		try {
			wym.setFocusToNode(node);
		} catch (e) { }
	}

	function insertContainer(html, toggle) {
		var block = $(this.findUp(this.container(), WYMeditor.BLOCKS)).get(0);
		var node = $(this.findUp(this.container(), WYMeditor.MAIN_CONTAINERS)).get(0);
		if((!node || !node.parentNode) && block && block.parentNode) {
			var div = $(html);
			$(block).append(div);
			setFocusToNode(this, div.find('p')[0]);
		} else if(!node || !node.parentNode) {
			var div = $(html);
			$(this._doc.body).append(div);
			setFocusToNode(this, div.find('p')[0]);
		} else if (toggle && $(node.parentNode).is(toggle) && $(node.parentNode).children().length < 2) {
			$(node.parentNode).replaceWith(node);
			setFocusToNode(this, node);
		} else if (toggle && $(node.parentNode).is(toggle)) {
			$(node.parentNode).after(node);
			setFocusToNode(this, node);
		} else {
			var div = $(html);
			var clone = $(node).clone();
			div.find('p').replaceWith(clone);
			$(node).replaceWith(div);
			setFocusToNode(this, clone[0]);
		}
	}

	function insertField(html, toggle) {
		var block = $(this.findUp(this.container(), WYMeditor.BLOCKS)).get(0);
		var node = $(this.findUp(this.container(), WYMeditor.MAIN_CONTAINERS)).get(0);
		if (toggle && select(this.container(), toggle).length) {
			var field = select(this.container(), toggle);
			var parent = field.parent();
			field.remove();
			setFocusToNode(this, parent[0]);
		} else if ((!node || !node.parentNode) && block && block.parentNode) {
			this.insert(html);
		} else if (!node || !node.parentNode) {
			var div = $(html);
			$(this._doc.body).append(div);
			setFocusToNode(this, div);
		} else {
			var div = $(html);
			$(node).before(div);
			setFocusToNode(this, node);
		}
	}

	function insertInput(dialog_input, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('InsertInput', null, dialog_input);
		var node = select(this.container(), toggle)[0];
		if (node) {
			$('#label').val($(node).find('label').text());
			$('#curie').val($(node).find('[property]').attr('property'));
			$('#functional').attr('checked', 'checked');
		} else {
			$('.wym_dialog_input .wym_delete').remove();
		}
		$('.wym_dialog_input .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_input .wym_submit').parents('form').submit(function(event) {
			var label = $('#label').val();
			var functional = $('#functional').is(':checked');
			var curie = $('#curie').val();
			var variable = $('#variable').val();
			if (!checkCURIE(curie) || !checkVariable(variable)) {
				return false;
			}
			var cardinality = functional ? 'data-cardinality="1"' : '';
			insertTemplate(node, template, {type: type, label: label,
				id: variable.substring(1), curie: curie,
				cardinality: cardinality});
			closeDialogue();
			return false;
		});
	}

	function insertSelect(dialog_select, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('InsertSelect', null, dialog_select);
		var node = select(this.container(), toggle)[0];
		if (node) {
			$('#label').val($(node).find('label').text());
			$('#property').val($(node).find('[property]').attr('property'));
			$('#variable').val($(node).find('[resource]').attr('resource'));
			if ($(node).find('[rev]').length) {
				$('#reverse').attr('checked', 'checked');
				$('#curie').val($(node).find('[rev]').attr('rev'));
			} else {
				$('#curie').val($(node).find('[rel]').attr('rel'));
			}
			var scheme = $(node).find('[rel=skos:inScheme]').attr('resource');
			var options = $('#scheme')[0].options;
			for (var i = 0; i < options.length; i++) {
				if (scheme == options[i].getAttribute('about')) {
					$('#scheme')[0].selectedIndex = i;
				}
			}
		} else {
			$('.wym_dialog_select .wym_delete').remove();
		}
		$('.wym_dialog_select .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_select .wym_submit').parents('form').submit(function(event) {
			var attr = $('#reverse').is(':checked') ? 'rev' : 'rel';
			var label = $('#label').val();
			var curie = $('#curie').val();
			var variable = $('#variable').val();
			var property = $('#property').val();
			var select = $('#scheme')[0];
			var scheme = select.options[select.selectedIndex].getAttribute('about');
			if (!checkCURIE(curie) || !checkVariable(variable)) {
				return false;
			}
			insertTemplate(node, template, {type: type, label: label,
				id: variable.substring(1), rel: attr, curie: curie,
				scheme: scheme, property: property});
			closeDialogue();
			return false;
		});
	}

	function insertDropArea(dialog_drop, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('InsertDropArea', null, dialog_drop);
		var node = select(this.container(), toggle)[0];
		if (node) {
			$('#label').val($(node).find('label').text());
			$('#property').val($(node).find('[property]').attr('property'));
			$('#variable').val($(node).find('[about]').attr('about'));
			if ($(node).find('[rev]').length) {
				$('#reverse').attr('checked', 'checked');
				$('#curie').val($(node).find('[rev]').attr('rev'));
			} else {
				$('#curie').val($(node).find('[rel]').attr('rel'));
			}
			$('#class').val($(node).find('[typeof]').attr('typeof'));
			$('#prompt').val($(node).find('[data-prompt]').attr('data-prompt'));
		} else {
			$('.wym_dialog_drop .wym_delete').remove();
		}
		$('.wym_dialog_drop .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_drop .wym_submit').parents('form').submit(function(event) {
			var attr = $('#reverse').is(':checked') ? 'rev' : 'rel';
			var label = $('#label').val();
			var curie = $('#curie').val();
			var variable = $('#variable').val();
			var property = $('#property').val();
			var ctype = $('#class').val();
			var prompt = $('#prompt').val();
			if (!checkCURIE(curie) || !checkVariable(variable) || !checkCURIE(ctype)) {
				return false;
			}
			insertTemplate(node, template, {type: type, label: label,
				id: variable.substring(1), rel: attr, curie: curie,
				'class': ctype, property: property, prompt: prompt});
			closeDialogue();
			return false;
		});
	}

	function insertTemplate(node, template, bindings) {
		var wym = window.WYMeditor.INSTANCES[0];
		var html = template;
		for (key in bindings) {
			html = html.replace(new RegExp('\\$' + key + '\\b', 'g'), bindings[key]);
		}
		var label = '<label for="' + bindings['id'] + '">' + bindings['label'] + '</label>\n\t';
		html = '<div class="' + bindings['type'] + ' field">\n\t' + label + html + '\n</div>\n';
		if (node && node.parentNode) {
			var div = $(html);
			$(node).replaceWith(div);
			setFocusToNode(wym, div);
		} else {
			var toggle = 'div.field.' + bindings['type'];
			insertField.call(wym, html, toggle);
		}
	}

	function insertRel(dialog_rel) {
		var wym = this;
		this.dialog('InsertRel', null, dialog_rel);
		var blocks = new Array("address", "div", "dl", "fieldset", "form", "noscript", "ol", "ul", "dd", "dt", "li", "tr");
		var node = jQuery(this.findUp(this.container(), blocks)).get(0);
		if (node) {
			if ($(node).is("*[rev]")) {
				$('#reverse').attr('checked', 'checked');
				$('#curie').val($(node).attr('rev'));
			} else {
				$('#curie').val($(node).attr('rel'));
			}
			$('#variable').val($(node).attr('resource'));
		}
		$('.wym_dialog_rel .wym_submit').parents('form').submit(function(event) {
			var attr = $('#reverse').is(':checked') ? 'rev' : 'rel';
			var curie = $('#curie').val();
			var variable = $('#variable').val();
			if (!checkCURIE(curie) || !checkVariable(variable)) {
				return false;
			}
			if (node && node.parentNode) {
				$(node).removeAttr('rel');
				$(node).removeAttr('rev');
				$(node).attr(attr, curie);
				$(node).attr('resource', variable);
			} else {
				node = jQuery(wym.findUp(wym.container(), WYMeditor.MAIN_CONTAINERS)).get(0);
				if (node && node.parentNode) {
					var div = $('<div/>');
					div.attr('class', 'vbox');
					div.attr(attr, curie);
					div.attr('resource', variable);
					div.append($(node).clone());
					$(node).replaceWith(div);
					setFocusToNode(wym, div.children()[0]);
				} else {
					var div = '<div ' + attr + '="' + curie + '" resource="' + variable + '"><p>{rdfs:label}</p></div>';
					$(wym._doc.body).append(div);
					setFocusToNode(wym, div.children()[0]);
				}
			}
			closeDialogue();
			return false;
		});
	}

	function checkCURIE(curie, form) {
		if (curie && curie.match(/^\w+:\w+$/))
			return true;
		$(form).trigger('calliError', 'CURIE must have be a word prefix and word suffix separated by a colon');
		return false;
	}

	function checkVariable(variable, form) {
		if (variable && variable.match(/^\?[a-zA-Z]\w*$/))
			return true;
		$(form).trigger('calliError', 'Variables must start with "?" and be alphanumeric');
		return false;
	}

	function closeDialogue() {
		if (window.opener) {
			window.close();
		} else if (window.dialog) {
			window.dialog.dialog('close');
		}
		$(this).trigger('calliSuccess');
	}

	function formatXML(text) {
		if (window.XML) {
			if (text.search(/^<\?xml/) == 0) {
				text = text.substring(text.indexOf('?>') + 2)
			}
			return window.XML(text).toXMLString()
		}
		var sheet ='<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"><xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/><xsl:template match="node()|@*"><xsl:copy><xsl:apply-templates select="node()|@*"/></xsl:copy></xsl:template></xsl:stylesheet>'
		var xsl = new DOMParser().parseFromString(sheet, "text/xml")
		var src = new DOMParser().parseFromString(text, "text/xml")
		var xsltproc = new XSLTProcessor()
		xsltproc.importStylesheet(xsl)
		var result = xsltproc.transformToDocument(src)
		return new XMLSerializer().serializeToString(result)
	}

	window.WYMeditor.INSTANCES[0].initHtml = function(html) {
		var wym = window.WYMeditor.INSTANCES[0];

		var innerHtml = wym.html;
		wym.html = function(html) {
			if(typeof html === 'string') {
				var ret = innerHtml.call(this, html);
				$(wym._doc.body).find(':input').attr("disabled", 'disabled');
				$(wym._doc.body).find('.vbox:not(:has(p))').append('<p></p>');
				$(wym._doc.body).find('.hbox:not(:has(p))').append('<p></p>');
				return ret;
			} else {
				$(wym._doc.body).find(':input').removeAttr("disabled");
				var markup = $(this._doc.body).html();
				$(wym._doc.body).find(':input').attr("disabled", 'disabled');
				return markup;
			}
		};

		var innerXhtml = wym.xhtml;
		wym.xhtml = function(html) {
			var xhtml = innerXhtml.call(this, html);
			var formatted = formatXML('<body>' + xhtml + '</body>');
			return formatted.replace(/^\s*<body\s*>/, '').replace(/<\/body>\s*$/, '');
		};

		var innerKeyup = wym.keyup;
		wym.keyup = function(event) {
			var ret = innerKeyup.call(this, event);
			$(wym._doc.body).find('.vbox:not(:has(p))').append('<p></p>');
			$(wym._doc.body).find('.hbox:not(:has(p))').append('<p></p>');
			return ret;
		};

		var previously = wym.exec;
		var dialog_input = outter($('.wym_dialog_input'));
		var dialog_select = outter($('.wym_dialog_select'));
		var dialog_drop = outter($('.wym_dialog_drop'));
		var dialog_rel = outter($('.wym_dialog_rel'));

		wym.exec = function(cmd) {
			switch (cmd) {
			case 'InsertVBox':
				insertContainer.call(wym, '<div class="vbox">\n<p></p></div>\n', 'div.vbox');
			break;
			case 'InsertHBox':
				insertContainer.call(wym, '<div class="hbox">\n<p></p></div>\n', 'div.hbox');
			break;
			case 'InsertForm':
				var form = $('<form about="?this" action="">\n<div class="vbox">\n<p></p></div>\n</form>\n');
				form.append('<button type="submit" disabled="disabled">Create</button>\n');
				form.append('<button type="submit" disabled="disabled">Save</button>\n');
				form.append('<button type="button" disabled="disabled" onclick="location.replace(\'?view\')">Cancel</button>\n');
				form.append('<button type="button" disabled="disabled" onclick="calli.deleteResource(form)">Delete</button>\n');
				insertContainer.call(wym, form, 'form');
			break;
			case 'InsertInput':
				insertInput.call(wym, dialog_input, 'input', '<div $cardinality><input id="$id" disabled="disabled" property="$curie" /></div>');
			break;
			case 'InsertTextArea':
				insertInput.call(wym, dialog_input, 'text', '<div $cardinality><textarea id="$id" disabled="disabled" property="$curie"></textarea></div>');
			break;
			case 'InsertRadio':
				insertSelect.call(wym, dialog_select, 'radio', '<div id="$id"><label $rel="$curie" resource="?$id"><input type="radio" name="$id" checked="checked" /><span rel="skos:inScheme" resource="$scheme" property="$property" /></label></div>');
			break;
			case 'InsertCheckbox':
				insertSelect.call(wym, dialog_select, 'checkbox', '<div id="$id"><label $rel="$curie" resource="?$id"><input type="checkbox" name="$id" checked="checked" /><span rel="skos:inScheme" resource="$scheme" property="$property" /></label></div>');
			break;
			case 'InsertDropDown':
				insertSelect.call(wym, dialog_select, 'dropdown', '<div><select id="$id"><option $rel="$curie" resource="?$id"><span rel="skos:inScheme" resource="$scheme" property="$property" /></option></div>');
			break;
			case 'InsertSelect':
				insertSelect.call(wym, dialog_select, 'select', '<div><select multiple="multiple" id="$id"><option $rel="$curie" resource="?$id"><span rel="skos:inScheme" resource="$scheme" property="$property" /></option></div>');
			break;
			case 'InsertDropArea':
				insertDropArea.call(wym, dialog_drop, 'droparea', '<div id="$id" $rel="$curie" data-prompt="$prompt"><div about="?$id" typeof="$class"><span property="$property" /></div></div>');
			break;
			case 'InsertRel':
				insertRel.call(wym, dialog_rel);
			break;
			default:
				return previously.call(wym, cmd);
			}
		};

		jQuery.wymeditors(0)._html = html;
		wym.initIframe($('#iframe')[0]);
		$('#iframe').trigger('load');
		setTimeout(function(){
			if (!$('body', $('#iframe')[0].contentWindow.document).children().length) {
				// IE need this called twice on first load (reload doesn't seem to work)
				wym.initIframe($('#iframe')[0]);
			}
		}, 1000);
	}
});
