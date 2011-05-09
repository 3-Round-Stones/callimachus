// page-edit-wym.js

jQuery(function($){

	function outter(node) {
		var html = $($('<div/>').html($(node).clone())).html();
		$(node).remove();
		return html;
	}

	$(document).ready(function() {
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

	function insertInput(dialog_input, html, toggle) {
		var wym = this;
		this.dialog('InsertInput', null, dialog_input);
		var node = select(this.container(), toggle)[0];
		if (node) {
			$('#label').val($(node).find('label').text());
			$('#curie').val($(node).find('[property]').attr('property'));
			$('#functional').attr('checked', 'checked');
		}
		$('.wym_dialog_input .wym_submit').parents('form').submit(function(event) {
			var label = $('#label').val();
			var functional = $('#functional').is(':checked');
			var curie = $('#curie').val();
			var variable = $('#variable').val();
			if (!curie || curie.indexOf(':') < 1) {
				$(this).trigger('calliError', 'Invalid CURIE');
				return false;
			}
			if (!variable || variable.indexOf('?') != 0) {
				$(this).trigger('calliError', 'Variables must start with "?"');
				return false;
			}
			html = html.replace(/@@LABEL@@/g, label);
			html = html.replace(/@@ID@@/g, variable.substring(1));
			html = html.replace(/@@CURIE@@/g, curie);
			if (node && node.parentNode) {
				var div = $(html);
				$(node).replaceWith(div);
				setFocusToNode(wym, div);
			} else {
				insertField.call(wym, html, toggle);
			}
			if (window.opener) {
				window.close();
			} else if (window.dialog) {
				window.dialog.dialog('close');
			}
			$(this).trigger('calliSuccess');
			return false;
		});
	}

	function insertSelect(dialog_select, html, toggle) {
		var wym = this;
		this.dialog('InsertSelect', null, dialog_select);
		var node = select(this.container(), toggle)[0];
		if (node) {
			$('#label').val($(node).find('label').text());
			$('#curie').val($(node).find('[property]').attr('property'));
			$('#functional').attr('checked', 'checked');
		}
		$('.wym_dialog_input .wym_submit').parents('form').submit(function(event) {
			var label = $('#label').val();
			var functional = $('#functional').is(':checked');
			var curie = $('#curie').val();
			var variable = $('#variable').val();
			if (!curie || curie.indexOf(':') < 1) {
				$(this).trigger('calliError', 'Invalid CURIE');
				return false;
			}
			if (!variable || variable.indexOf('?') != 0) {
				$(this).trigger('calliError', 'Variables must start with "?"');
				return false;
			}
			html = html.replace(/@@LABEL@@/g, label);
			html = html.replace(/@@ID@@/g, variable.substring(1));
			html = html.replace(/@@CURIE@@/g, curie);
			if (node && node.parentNode) {
				var div = $(html);
				$(node).replaceWith(div);
				setFocusToNode(wym, div);
			} else {
				insertField.call(wym, html, toggle);
			}
			if (window.opener) {
				window.close();
			} else if (window.dialog) {
				window.dialog.dialog('close');
			}
			$(this).trigger('calliSuccess');
			return false;
		});
	}

	function insertRel(dialog_rel) {
		var wym = this;
		this.dialog('InsertRel', null, dialog_rel);
		var blocks = new Array("address", "div", "dl", "fieldset", "form", "noscript", "ol", "ul", "dd", "dt", "li", "tr");
		var node = jQuery(this.findUp(this.container(), blocks)).get(0);
		if (node && $(node).is("*[rel]")) {
			$('#curie').val($(node).attr('rel'));
			$('#variable').val($(node).attr('resource'));
		} else if (node && $(node).is("*[rev]")) {
			$('#reverse').attr('checked', 'checked');
			$('#curie').val($(node).attr('rev'));
			$('#variable').val($(node).attr('resource'));
		}
		$('.wym_dialog_rel .wym_submit').parents('form').submit(function(event) {
			var attr = $('#reverse').is(':checked') ? 'rev' : 'rel';
			var curie = $('#curie').val();
			var variable = $('#variable').val();
			if (!curie || curie.indexOf(':') < 1) {
				$(this).trigger('calliError', 'Invalid CURIE');
				return false;
			}
			if (!variable || variable.indexOf('?') != 0) {
				$(this).trigger('calliError', 'Variables must start with "?"');
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
			if (window.opener) {
				window.close();
			} else if (window.dialog) {
				window.dialog.dialog('close');
			}
			$(this).trigger('calliSuccess');
			return false;
		});
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
		var dialog_rel = outter($('.wym_dialog_rel'));

		wym.exec = function(cmd) {
			switch (cmd) {
			case 'InsertVBox':
				insertContainer.call(wym, '<div class="vbox"><p></p></div>\n', 'div.vbox');
			break;
			case 'InsertHBox':
				insertContainer.call(wym, '<div class="hbox"><p></p></div>\n', 'div.hbox');
			break;
			case 'InsertForm':
				var form = $('<form about="?this" action="">\n<p></p></form>\n');
				form.append('<button type="submit" disabled="disabled">Create</button>\n');
				form.append('<button type="submit" disabled="disabled">Save</button>\n');
				form.append('<button type="button" disabled="disabled" onclick="location.replace(\'?view\')">Cancel</button>\n');
				form.append('<button type="button" disabled="disabled" onclick="calli.deleteResource(form)">Delete</button>\n');
				insertContainer.call(wym, form, 'form');
			break;
			case 'InsertInput':
				insertInput.call(wym, dialog_input, '<div class="input field"><label for="@@ID@@">@@LABEL@@</label><div><input id="@@ID@@" disabled="disabled" property="@@CURIE@@" /></div></div>\n', 'div.input.field');
			break;
			case 'InsertTextArea':
				insertInput.call(wym, dialog_input, '<div class="text field"><label for="@@ID@@">@@LABEL@@</label><div><textarea id="@@ID@@" disabled="disabled" property="@@CURIE@@" /></div></div>\n', 'div.text.field');
			break;
			case 'InsertRadio':
				insertSelect.call(wym, dialog_select, '<div class="radio field"><label for="@@ID@@">@@LABEL@@</label><div id="@@ID@@"><label @@REL@@="@@CURIE@@" resource="@@VAR@@"><input type="radio" name="@@ID@@" checked="checked" /><span rel="skos:inScheme" resource="@@SCHEME@@" property="@@PROPERTY@@" /></label></div></div>\n', 'div.radio.field');
			break;
			case 'InsertCheckbox':
				insertSelect.call(wym, dialog_select, '<div class="radio field"><label for="@@ID@@">@@LABEL@@</label><div id="@@ID@@"><label @@REL@@="@@CURIE@@" resource="@@VAR@@"><input type="checkbox" name="@@ID@@" checked="checked" /><span rel="skos:inScheme" resource="@@SCHEME@@" property="@@PROPERTY@@" /></label></div></div>\n', 'div.radio.field');
			break;
			case 'InsertDropDown':
				insertSelect.call(wym, dialog_select, '<div class="radio field"><label for="@@ID@@">@@LABEL@@</label><div><select id="@@ID@@"><option @@REL@@="@@CURIE@@" resource="@@VAR@@"><span rel="skos:inScheme" resource="@@SCHEME@@" property="@@PROPERTY@@" /></option></div></div>\n', 'div.dropdown.field');
			break;
			case 'InsertSelect':
				insertSelect.call(wym, dialog_select, '<div class="radio field"><label for="@@ID@@">@@LABEL@@</label><div><select multiple="multiple" id="@@ID@@"><option @@REL@@="@@CURIE@@" resource="@@VAR@@"><span rel="skos:inScheme" resource="@@SCHEME@@" property="@@PROPERTY@@" /></option></div></div>\n', 'div.dropdown.field');
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
