// jquery.wymeditor.callimachus.js

/*
 * Support inserting of Callimachus form elements into page
 */
(function($){

	function outer(node) {
		var html = $($('<div/>').html($(node).clone())).html();
		$(node).remove();
		return html;
	}

	function select(node, selector) {
		if (!node)
			return $([]);
		var set = $(node).find(selector).andSelf();
		set = set.add($(node).parents(selector));
		return set.filter(selector);
	}

	function find(node, selector) {
		if (!node)
			return $([]);
		return $(node).find(selector).andSelf().filter(selector);
	}

	function setFocusToNode(wym, node) {
		updateBody();
		try {
			wym.setFocusToNode(node);
		} catch (e) { }
	}

	function insertContainer(html, toggle) {
		var block = this.findUp(this.container(), WYMeditor.BLOCKS);
		var node = this.findUp(this.container(), WYMeditor.MAIN_CONTAINERS);
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

	function insertField(html) {
		var container = this.container();
		var field = $(container).parents().andSelf().filter('.field')[0];
		var block = this.findUp(container, WYMeditor.BLOCKS);
		var node = this.findUp(container, WYMeditor.MAIN_CONTAINERS);
		if (field) {
			var div = $(html);
			$(field).after('\n\t\t');
			$(field).after(div);
			$(field).after('\n\t\t\t');
			setFocusToNode(this, div[0]);
		} else if (node) {
			var div = $(html);
			$(node).before('\n\t\t\t');
			$(node).before(div);
			$(node).before('\n\t\t');
			setFocusToNode(this, node);
		} else if ($(container).is(':input')) {
			var div = $(html);
			$(container).before('\n\t\t\t');
			$(container).before(div);
			$(container).before('\n\t\t');
			setFocusToNode(this, node);
		} else if (block && block.parentNode) {
			this.insert(html);
			this.update();
		} else {
			var div = $(html);
			$(this._doc.body).append('\n\t\t\t');
			$(this._doc.body).append(div);
			$(this._doc.body).append('\n\t\t');
			setFocusToNode(this, div[0]);
		}
	}

	function insertInput(dialog_input, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('Insert Input', null, dialog_input);
		var node = select(this.container(), toggle)[0];
		var container = node ? $(node).parent()[0] : this.container();
		if (node) {
			$('#label').val(find(node, 'label').text());
			setCurie('#prefix', '#local', $(node).html().match(/\{([^}:]*:[^}]*)\}/)[1]);
		} else {
			$('.wym_dialog_input .wym_delete').remove();
			updatePrefixSelect('#prefix');
		}
		$('#label').change(function(event) {
			var value = $(this).val();
			var local = value.substring(0, 1).toLowerCase() + value.substring(1).replace(/\W/g, '');
			$('#local').val(local);
		});
		$('.wym_dialog_input .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_input .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var label = $('#label').val();
			var curie = getCurie('#prefix', '#local', this, true);
			if (!curie)
				return false;
			var id = findOrGenerateId(curie, container);
			insertTemplate(template, {type: type, label: label,
				id: id, curie: curie});
			closeDialogue();
			return false;
		});
	}

	function insertSelect(dialog_select, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('Insert Select', null, dialog_select);
		var node = select(this.container(), toggle)[0];
		var container = node ? $(node).parent()[0] : this.container();
		$('#scheme').change(function(event) {
			var label = $($('#scheme')[0].options[$('#scheme')[0].selectedIndex]).text();
			var local = label.replace(/\W/g, '');
			$('#local').val('has' + local);
		});
		jQuery.get('/callimachus/Page?schemes', function(xml) {
			$(xml).find('result').each(function() {
				var uri = $(this).find('[name=uri]>*').text();
				var label = $(this).find('[name=label]>*').text();
				var option = $('<option/>');
				option.attr('value', uri);
				option.text(label);
				$('#scheme').append(option);
			});
			if (node) {
				var scheme = find(node, '[rel=skos:inScheme]').attr('resource');
				var options = $('#scheme')[0].options;
				for (var i = 0; i < options.length; i++) {
					if (scheme == options[i].getAttribute('value')) {
						$('#scheme')[0].selectedIndex = i;
					}
				}
				$('#scheme').change();
				setCurie('#prefix', '#local', find(node, '[rel]').attr('rel'));
			} else {
				updatePrefixSelect('#prefix');
				$('#scheme').change();
				$('.wym_dialog_select .wym_delete').remove();
			}
		}, 'xml');
		$('.wym_dialog_select .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_select .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var attr = 'rel';
			var label = $($('#scheme')[0].options[$('#scheme')[0].selectedIndex]).text();
			var curie = getCurie('#prefix', '#local', this, true);
			var scheme = $('#scheme').val();
			if (!scheme) {
				$(this).trigger('calliError', 'A scheme is required for this field');
			}
			if (!curie)
				return false;
			var id = findOrGenerateId(curie, container);
			insertTemplate(template, {type: type, label: label,
				id: id, rel: attr, curie: curie,
				scheme: scheme});
			closeDialogue();
			return false;
		});
	}

	function insertDropZone(dialog_drop, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('Insert Drop Zone', null, dialog_drop);
		var node = select(this.container(), toggle)[0];
		var container = node ? $(node).parent()[0] : this.container();
		if (node) {
			$('#label').val(find(node, 'label').text());
			setCurie('#prefix', '#local', find(node, '[rel]').attr('rel'));
			setCurie('#class-prefix', '#class-local', find(node, '[typeof]').attr('typeof'));
			$('#prompt').val(find(node, '[data-dialog]').attr('data-dialog'));
		} else {
			$('.wym_dialog_dropzone .wym_delete').remove();
			updatePrefixSelect('#prefix');
			updatePrefixSelect('#class-prefix');
		}
		$('#label').change(function(event) {
			var label = $('#label').val();
			var local = label.replace(/\W/g, '');
			$('#local').val('has' + local);
		});
		$('.wym_dialog_dropzone .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_dropzone .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var attr = 'rel';
			var label = $('#label').val();
			var curie = getCurie('#prefix', '#local', this, true);
			var ctype = getCurie('#class-prefix', '#class-local', this, true);
			var prompt = $('#prompt').val();
			if (!curie || !ctype)
				return false;
			var id = findOrGenerateId(curie, container);
			insertTemplate(template, {type: type, label: label,
				id: id, rel: attr, curie: curie,
				'class': ctype, prompt: prompt});
			closeDialogue();
			return false;
		});
	}

	function insertTemplate(template, bindings) {
		var wym = window.WYMeditor.INSTANCES[0];
		var html = template;
		for (key in bindings) {
			var value = bindings[key].replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&apos;');
			html = html.replace(new RegExp('\\$' + key + '\\b', 'g'), value);
		}
		if (html.indexOf('class="' + bindings['type'] + ' field"') < 0) {
			var label = $('<label/>');
			label.attr('for', bindings['id']);
			label.text(bindings['label']);
			var div = $('<div/>');
			div.attr('class', bindings['type'] + ' field');
			div.append('\n\t\t\t\t');
			div.append(label);
			div.append('\n\t\t\t\t');
			div.append(html);
			div.append('\n\t\t\t');
			html = $('<div/>').append(div).html();
		}
		insertField.call(wym, html);
	}

	function insertForm(dialog_form) {
		var wym = this;
		this.dialog('Insert Form', null, dialog_form);
		var node = this.findUp(this.container(), ['form']);
		if (node) {
			setCurie('#prefix', '#local', $(node).attr('typeof'));
			if ($(node).find('button[onclick]').length) {
				$('#create-radio')[0].checked = false;
				$('#edit-radio')[0].checked = true;
			} else {
				$('#edit-radio')[0].checked = false;
				$('#create-radio')[0].checked = true;
			}
		} else {
			$('.wym_dialog_form .wym_delete').remove();
			updatePrefixSelect('#prefix');
		}
		$('.wym_dialog_form .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_form .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var curie = getCurie('#prefix', '#local', this, false);
			var form = $('<form/>');
			form.attr("about", '?this');
			if (curie) {
				form.attr('typeof', curie);
			} else {
				form.removeAttr("typeof");
			}
			var focus = null;
			if (node) {
				form.append($(node).children(':not(:button)'));
				focus = form.children(':not(:button)')[0];
			} else {
				form.append('\n\t\t');
				form.append('<div class="vbox">\n\t\t\t<p></p>\n\t\t</div>');
				form.append('\n');
				focus = form.find('p')[0];
			}
			if ($('#create-radio').is(':checked')) {
				form.append('\t\t<button type="submit">Create</button>\n\t');
			} else {
				form.append('\t\t<button type="submit">Save</button>\n');
				form.append('\t\t<button type="button" onclick="location.replace(\'?view\')">Cancel</button>\n');
				form.append('\t\t<button type="button" onclick="calli.deleteResource(form)">Delete</button>\n\t');
			}
			if (node) {
				$(node).replaceWith(form);
				if (focus) {
					setFocusToNode(wym, focus);
				}
			} else {
				insertContainer.call(wym, form, 'form');
			}
			closeDialogue();
			return false;
		});
	}

	function addProperty(dialog_property) {
		var wym = this;
		this.dialog('Add Property', null, dialog_property);
		var blocks = new Array("span", "pre");
		var node = this.findUp(this.container(), blocks);
		var container = node ? $(node).parent()[0] : this.container();
		if (node && $(node).attr("property")) {
			setCurie('#prefix', '#local', $(node).attr("property"));
			$('#variable').val($(node).attr("content"));
		} else {
			$('.wym_dialog_property .wym_delete').remove();
			updatePrefixSelect('#prefix');
		}
		$('#local').change(function(event) {
			$('#variable').val('?' + findOrGenerateId(getCurie('#prefix', '#local', this.form, false), container));
		});
		$('.wym_dialog_property .wym_delete').click(function(event) {
			if ($(node).is('span')) {
				$(node).replaceWith($(node).html().replace('{' + $(node).attr("content") + '}', ''));
			} else {
				$(node).html($(node).html().replace('{' + $(node).attr("content") + '}', ''));
				$(node).removeAttr("property");
				$(node).removeAttr("content");
			}
			closeDialogue();
		});
		$('.wym_dialog_property .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var curie = getCurie('#prefix', '#local', this, true);
			var variable = $('#variable').val();
			if (!curie || !checkVariable(variable, this)) {
				return false;
			}
			var id = variable.substring(1);
			if (node && ($(node).is('pre') || $(node).attr("property"))) {
				$(node).attr("property", curie);
				$(node).attr("content", '?' + id);
				if ($(node).text().match(/\{.*\}/)) {
					$(node).text($(node).text().replace(/\{.*\}/, '{?' + id + '}'));
				} else {
					wym.insert('{?' + id + '}');
				}
			} else {
				wym.insert('<span property="' + curie + '" content="?' + id + '">{?' + id + '}</span>');
			}
			closeDialogue();
			return false;
		});
	}

	function addRel(dialog_rel) {
		var wym = this;
		this.dialog('Add Rel', null, dialog_rel);
		var blocks = new Array("address", "div", "dl", "fieldset", "form", "noscript", "ol", "ul", "dd", "dt", "li", "tr", "a");
		var node = this.findUp(this.container(), blocks);
		var container = node ? $(node).parent()[0] : this.container();
		if (node && $(node).attr('rel')) {
			setCurie('#prefix', '#local', $(node).attr('rel'));
			$('#variable').val($(node).attr("resource"));
		} else {
			$('.wym_dialog_rel .wym_delete').remove();
			updatePrefixSelect('#prefix');
		}
		$('#local').change(function(event) {
			$('#variable').val('?' + findOrGenerateId(getCurie('#prefix', '#local', this.form, false), container));
		});
		$('.wym_dialog_rel .wym_delete').click(function(event) {
			$(node).removeAttr('rel');
			$(node).removeAttr('resource');
			closeDialogue();
		});
		$('.wym_dialog_rel .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var attr = 'rel';
			var curie = getCurie('#prefix', '#local', this, true);
			var variable = $('#variable').val();
			if (!curie || !checkVariable(variable, this)) {
				return false;
			}
			var id = variable.substring(1);
			if (node && node.parentNode) {
				$(node).removeAttr('rel');
				$(node).removeAttr('rev');
				$(node).attr(attr, curie);
				$(node).attr('resource', '?' + id);
			} else {
				node = wym.findUp(wym.container(), WYMeditor.MAIN_CONTAINERS);
				if (node && node.parentNode) {
					var div = $('<div/>');
					div.attr('class', 'vbox');
					div.attr(attr, curie);
					div.attr('resource', '?' + id);
					div.append($(node).clone());
					$(node).replaceWith(div);
					setFocusToNode(wym, div.children()[0]);
				} else {
					var div = '<div class="vbox"><p>{rdfs:label}</p></div>';
					div.attr(attr, curie);
					div.attr('resource', '?' + id);
					$(wym._doc.body).append(div);
					setFocusToNode(wym, div.children()[0]);
				}
			}
			closeDialogue();
			return false;
		});
	}

	function setCurie(prefix, local, curie) {
		var val = null;
		if (curie) {
			var m = curie.match(/([^:]*):(.*)/);
			if (m) {
				val = m[1];
				$(local).val(m[2]);
			}
		}
		updatePrefixSelect(prefix, val);
	}

	function getCurie(prefix, local, form, required) {
		var curie = $(prefix).val() + ':' + $(local).val();
		if ((curie != ':' || required) && checkCURIE(curie, form))
			return curie;
		return null;
	}

	function updatePrefixSelect(ref, prefix) {
		jQuery.getJSON('/callimachus/profile', function(json) {
			var items = [];
			$.each(json, function(key, val) {
				items.push(key);
			});
			$.each(items.sort(), function(index, prefix) {
				var option = $('<option/>');
				option.text(prefix);
				$(ref).append(option);
			});
			if (prefix) {
				$(ref).val(prefix);
			}
		});
	}

	function checkCURIE(curie, form) {
		if (curie && curie.match(/^[0-9a-zA-Z_\.-]+:[0-9a-zA-Z_\.-]+$/))
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

	function findOrGenerateId(curie, container) {
		if (!curie)
			return '';
		var wym = window.WYMeditor.INSTANCES[0];
		var path = [];
		var parents = $(container).add($(container).parents());
		if (parents.is('.field')) {
			parents = parents.filter('.field').parents();
		}
		parents.filter('[rel],[rev],[property],[id]').each(function() {
			var id = $(this).attr('id');
			if (id) {
				path = [id];
			} else {
				var rel = $(this).attr('rel');
				var rev = $(this).attr('rev');
				var property = $(this).attr('property');
				if (rel) {
					path.push(rel);
				}
				if (rev) {
					path.push(rev);
				}
				if (property) {
					path.push(property);
				}
			}
		});
		path.push(curie);
		var counter = '';
		var base = path.join('_').replace(/[^_]*\W+/g, '');
		while ($('#' + base + counter, wym._doc).length) {
			if (counter) {
				counter = counter + 1;
			} else {
				counter = 1;
			}
		}
		return base + counter;
	}

	function closeDialogue() {
		if (window.opener) {
			window.close();
		} else if (window.dialog) {
			window.dialog.dialog('close');
		}
		var wym = window.WYMeditor.INSTANCES[0];
		wym.update();
		$(document).trigger('calliSuccess');
	}

	function updateBody() {
		var wym = window.WYMeditor.INSTANCES[0];
		wym.update();
	}

var _wymeditor = jQuery.fn.wymeditor;
jQuery.fn.wymeditor = function(options) {
	var _postInit = options.postInit;
	options.postInit = function(wym) {

		$(wym._doc.body).bind('click', function(event) {
			event.preventDefault();
			return false;
		});
		if (wym._doc.addEventListener) {
			wym._doc.addEventListener('click', function(event) {
				if (event.preventDefault) {
					event.preventDefault();
				}
				if (event.stopPropagation) {
					event.stopPropagation();
				}
				return false;
			}, true);
		}

		var innerHtml = wym.html;
		wym.html = function(html) {
			if(typeof html === 'string') {
				updateBody();
				var index = jQuery.inArray(this.selected(), $(this._doc.body).find('*').get());
				var ret = innerHtml.call(this, html);
				updateBody();
				var nodes = $(this._doc.body).find('*');
				var node = nodes[index];
				if (node) {
					var next = nodes[index + 1];
					if (!$(node).is('p') && $(next).is('p')) {
						this.setFocusToNode(next);
					} else {
						this.setFocusToNode(node);
					}
				}
				return ret;
			} else {
				var markup = innerHtml.call(this, html);
				return markup;
			}
		};

		var innerKeyup = wym.keyup;
		wym.keyup = function(event) {
			var ret = innerKeyup.call(this, event);
			updateBody();
			return ret;
		};

		var dialog_input = outer($('.wym_dialog_input'));
		var dialog_select = outer($('.wym_dialog_select'));
		var dialog_drop = outer($('.wym_dialog_dropzone'));
		var dialog_form = outer($('.wym_dialog_form'));
		var dialog_property = outer($('.wym_dialog_property'));
		var dialog_rel = outer($('.wym_dialog_rel'));

		var previously = wym.exec;
		wym.exec = function(cmd) {
			switch (cmd) {
			case 'InsertVBox':
				insertContainer.call(wym, '<div class="vbox">\n<p></p></div>\n', 'div.vbox');
			break;
			case 'InsertHBox':
				insertContainer.call(wym, '<div class="hbox">\n<p></p></div>\n', 'div.hbox');
			break;
			case 'InsertForm':
				insertForm.call(wym, dialog_form);
			break;
			case 'InsertInput':
				insertInput.call(wym, dialog_input, 'input', '<input id="$id" value="{$curie}" />');
			break;
			case 'InsertTextArea':
				insertInput.call(wym, dialog_input, 'text', '<textarea id="$id" class="auto-expand">{$curie}</textarea>');
			break;
			case 'InsertRadio':
				insertSelect.call(wym, dialog_select, 'radio', '<div id="$id">\n\t\t\t\t\t<label $rel="$curie" resource="?$id">\n\t\t\t\t\t\t<input type="radio" name="$id" checked="checked" />\n\t\t\t\t\t\t<span rel="skos:inScheme" resource="$scheme" property="skos:prefLabel" />\n\t\t\t\t\t</label>\n\t\t\t\t</div>');
			break;
			case 'InsertCheckbox':
				insertSelect.call(wym, dialog_select, 'checkbox', '<div id="$id">\n\t\t\t\t\t<label $rel="$curie" resource="?$id">\n\t\t\t\t\t\t<input type="checkbox" name="$id" checked="checked" />\n\t\t\t\t\t\t<span rel="skos:inScheme" resource="$scheme" property="skos:prefLabel" />\n\t\t\t\t\t</label>\n\t\t\t\t</div>');
			break;
			case 'InsertDropDown':
				insertSelect.call(wym, dialog_select, 'dropdown', '<select id="$id" $rel="$curie">\n\t\t\t\t\t<option about="?$id" rel="skos:inScheme" resource="$scheme" property="skos:prefLabel" selected="selected" />\n\t\t\t\t</select>');
			break;
			case 'InsertSelect':
				insertSelect.call(wym, dialog_select, 'select', '<select multiple="multiple" id="$id" $rel="$curie">\n\t\t\t\t\t<option about="?$id" rel="skos:inScheme" resource="$scheme" property="skos:prefLabel" selected="selected" />\n\t\t\t\t</select>');
			break;
			case 'InsertDropZone':
				insertDropZone.call(wym, dialog_drop, 'dropzone', '<div id="$id" class="$type field" $rel="$curie" dropzone="link s:text/uri-list">\n\t\t\t\t\t<label>$label</label>\n\t\t\t\t\t<button type="button" class="dialog" data-dialog="$prompt"/>\n\t\t\t\t\t<div about="?$id" typeof="$class">\n\t\t\t\t\t\t<span property="rdfs:label" />\n\t\t\t\t\t\t<button type="button" class="remove" />\n\t\t\t\t\t</div>\n\t\t\t\t</div>');
			break;
			case 'AddProperty':
				addProperty.call(wym, dialog_property);
			break;
			case 'AddRel':
				addRel.call(wym, dialog_rel);
			break;
			default:
				var ret = previously.call(wym, cmd);
				wym.update();
				return ret;
			}
			wym.update();
		};

		if (_postInit) {
			_postInit.call(this, wym);
		}
	};
	return _wymeditor.call(this, options);
}
})(jQuery);
