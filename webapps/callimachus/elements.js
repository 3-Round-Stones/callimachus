/*
   Copyright (c) 2009-2010 Zepheira LLC, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

var rdfnil = "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"

if (document.addEventListener) {
	document.addEventListener("DOMContentLoaded", initRDFaForms, false);
}  else if (window.attachEvent) {
    window.attachEvent("onload", initRDFaForms);
}

function initRDFaForms() {
	initElements($("form[about]"))
}

function initElements(form) {
	initInputPropertyElements(form)
	initSetElements(form)
	initListElements(form)
	initInputRelElements(form)
	initOptionElements(form)
}

function get(url, callback) {
	$.ajax({ url: url, success: function(data) {
		if (window.showSuccess) {
			showSuccess()
		}
		if (typeof data == 'string' && data.match(/^\s*$/)) {
			callback()
		} else {
			callback(data)
		}
	}, error: function(xhr, textStatus, errorThrown) {
		if (window.showError) {
			showError(xhr.statusText ? xhr.statusText : errorThrown ? errorThrown : textStatus, xhr.responseText)
		}
	}})
}

function initInputPropertyElements(form) {
	$(":input[property]", form).each(initInputElement)
	createAddPropertyButtons(form)
}

function createAddPropertyButtons(form) {
	$("[data-more][data-property]", form).each(function() {
		var parent = $(this)
		var property = parent.attr("data-property")
		var minOccurs = parent.attr("data-min-cardinality")
		var maxOccurs = parent.attr("data-max-cardinality")
		minOccurs = minOccurs ? minOccurs : parent.attr("data-cardinality")
		maxOccurs = maxOccurs ? maxOccurs : parent.attr("data-cardinality")
		var count = parent.children("[property='" + property + "']").size()
		var add = false;
		if (!maxOccurs || !minOccurs || parseInt(minOccurs) != parseInt(maxOccurs)) {
			add = $('<button type="button"/>')
			add.addClass("add")
			add.attr("data-property", property)
			add.text("»")
			add.click(function(){
				get(parent.attr("data-more"), function(data) {
					var input = $(data)
					add.before(input)
					input.each(initInputElement)
					updateButtonState(parent, property, minOccurs, maxOccurs)
				})
			})
			parent.append(add)
		}
		if (minOccurs) {
			var n = parseInt(minOccurs) - count
			for (var i=0; i<n; i++) {
				get(parent.attr("data-more"), function(data) {
					var input = $(data)
					if (add) {
						add.before(input)
					} else {
						parent.append(input)
					}
					input.each(initInputElement)
					updateButtonState(parent, property, minOccurs, maxOccurs)
				})
			}
		}
		updateButtonState(parent, property, minOccurs, maxOccurs)
	})
}

function initInputElement() {
	var input = $(this)
	var property = input.attr("property")
	input.attr("data-property", property)
	input.change(function(){
		var value = $(this).val()
		if (value) {
			$(this).attr("property", property)
			$(this).attr("content", value)
		} else {
			$(this).removeAttr("property")
			$(this).removeAttr("content")
		}
	})
	input.change()
	var parent = input.parent()
	var minOccurs = parent.attr("data-min-cardinality")
	var maxOccurs = parent.attr("data-max-cardinality")
	minOccurs = minOccurs ? minOccurs : parent.attr("data-cardinality")
	maxOccurs = maxOccurs ? maxOccurs : parent.attr("data-cardinality")
	var remove = $('<button type="button"/>')
	remove.addClass("remove")
	remove.attr("data-property", property)
	remove.text("×")
	remove.click(function(){
		input.remove()
		remove.remove()
		updateButtonState(parent, property, minOccurs, maxOccurs)
	})
	input.after(remove)
}

function updateButtonState(context, property, minOccurs, maxOccurs) {
	var add = context.children("button[class='add'][data-property='" + property + "']")
	var buttons = context.children("button[class='remove'][data-property='" + property + "']")
	if (buttons.size() <= minOccurs) {
		buttons.hide()
	} else {
		buttons.show()
	}
	if (buttons.size() >= maxOccurs) {
		add.hide()
	} else {
		add.show()
	}
}

function initInputRelElements(form) {
	$("[data-options]:not(select,optgroup)", form).each(function(i, node) {
		var script = $(node)
		var objects = script.children("[resource]")
		loadOptions(script, objects, "checked", function() {
			script.append($(this))
		})
	})
}

function initOptionElements(form) {
	$("select").each(function(i, node) {
		var select = $(node)
		var script = select.is("[data-options]") ? select : select.children("[data-options]")
		var rel = script.attr("data-rel")
		if (script.size() && rel) {
			select.change(function(){
				$("option:selected", this).each(function(i, option){
					var $option = $(option)
					$option.attr("rel", rel)
					$option.removeAttr("about")
				})
				$("option:not(:selected)", this).each(function(i, option){
					var $option = $(option)
					$option.removeAttr("rel")
					$option.attr("about", $option.attr("resource"))
				})
			})
			var objects = $("[resource]", select)
			script.each(function() {
				var optgroup = $(this)
				loadOptions(optgroup, objects, "selected", function() {
					optgroup.append($(this))
				}, function() {
					select.change()
				})
			})
		}
	})
}

function loadOptions(script, objects, selected, callback, refresh) {
	get(script.attr("data-options"), function(data) {
		var options = $(data).children()
		var rel = script.attr("data-rel")
		var inputs = $("input", options)
		options.removeAttr("rel")
		if (selected) {
			options.removeAttr(selected)
			inputs.removeAttr(selected)
		}
		if (objects.size() && selected) {
			objects.each(function(i, obj){
				var uri = $(obj).attr("resource")
				$(options.get()).filter("[resource='" + uri + "']").each(function() {
					var option = $(this)
					if (inputs.size()) {
						inputs.attr(selected, selected)
					} else {
						option.attr(selected, selected)
					}
					option.attr("rel", rel)
					$(obj).remove()
				})
			})
		}
		if (inputs.size()) {
			options.each(function() {
				var option = $(this)
				$("input", option).change(function(){
					if ($(this).is(':checked') && !option.attr("rel")) {
						options.attr("rel", rel)
						$("[resource] input", script).change()
					} else if (!$(this).is(':checked') && option.attr("rel")) {
						option.removeAttr("rel")
						$("[resource] input", script).change()
					}
				})
			})
		}
		options.each(function(i, node) {
			callback.call(this, i, node)
		})
	})
	if (refresh) {
		refresh()
	}
}

function findType(node, defaultType) {
	if (node.is("script")) {
		var template = createFromTemplate(node)
		if (template.attr("typeof")) {
			return $("body").curie(template.attr("typeof")).toString()
		}
	}
	if (node.attr("typeof"))
		return $("body").curie(node.attr("typeof")).toString()
	if (node.parent().size())
		return findType(node.parent(), defaultType)
	return defaultType
}

var iframe_counter = 0

function initSetElements(form) {
	createAddRelButtons(form)
	var tbody = $("[data-add]", form)
	tbody.children("[about]").each(initSetElement)
	tbody.each(function() {
		var list = $(this)
		var dragover = function(event) {
			if (!list.hasClass("drag-over")) {
				list.addClass("drag-over")
			}
			return stopPropagation(event)
		}
		var dragleave = function(event) {
			list.removeClass("drag-over")
			return stopPropagation(event)
		}
		if (this.addEventListener) {
			this.addEventListener("dragenter", dragover, false)
			this.addEventListener("dragover", dragover, false)
			this.addEventListener("dragleave", dragleave, false)
			this.addEventListener("drop", pasteURL(dragleave), false)
		} else if (this.attachEvent) {
			this.attachEvent("ondragenter", dragover)
			this.attachEvent("ondragover", dragover)
			this.attachEvent("ondragleave", dragleave)
			this.attachEvent("ondrop", pasteURL(dragleave))
		}
		var url = list.attr("data-search")
		if (url && list.attr("data-suggest")) {
			var id = "lookup" + (++iframe_counter) + "-button"
			var name = "lookup" + (++iframe_counter) + "-iframe"
			var searchTerms = "searchTerms" + (++iframe_counter) + "-input"
			var add = $('<button type="button"/>')
			add.attr("class", "add")
			add.attr("id", id)
			add.text("»")
			var suggest = list.attr("data-suggest")
			var divert = window.diverted && list.hasClass("diverted")
			if (divert) {
				suggest = diverted(suggest, list.get(0))
			}
			var onsubmit = "listSearchResults('" + url + "'.replace('{searchTerms}', encodeURIComponent(document.getElementById('"
			+ searchTerms + "').value)), '" + name + "', " + (divert ? "true" : "false") + ")"
			var content = "<form action='' target='" + name + "' onsubmit=\"" + onsubmit + ";return false\">"
				+ "<div class='lookup'>"
				+ "<a href='#' class='close' onclick=\"$('#"+id+"').qtip('hide');return false\">Х</a>"
				+ "<a href='#' class='back' onclick=\"frames['" + name + "'].history.back();return false\">«</a>"
				+ "<input name='q' id='" + searchTerms + "' /></div>"
				+ "</form><iframe name='" + name + "' frameborder='0' src='" + suggest + "'></iframe>"
			setTimeout(function() {
				var width = document.documentElement.clientWidth
				var offset = add.offset().left
				var position = 'topMiddle'
				if (offset < width / 3) {
					position = 'topLeft'
				} else if (offset > width * 2 / 3) {
					position = 'topRight'
				}
				add.qtip({
					content: { text: content },
					show: { when: { event: 'click' } },
					hide: { when: { event: 'unfocus' } },
					style: { name: 'blue', padding: 0, width: {max:width}, tip: true },
					position: { corner: { target: 'center', tooltip: position } }
				})
			}, 500)
			list.append(add)
		}
	})
}

function createAddRelButtons(form) {
	$("[data-more][rel]", form).each(function() {
		var parent = $(this)
		var property = parent.attr("rel")
		var minOccurs = parent.attr("data-min-cardinality")
		var maxOccurs = parent.attr("data-max-cardinality")
		minOccurs = minOccurs ? minOccurs : parent.attr("data-cardinality")
		maxOccurs = maxOccurs ? maxOccurs : parent.attr("data-cardinality")
		var count = parent.children().size()
		var add = false;
		if (!maxOccurs || !minOccurs || parseInt(minOccurs) != parseInt(maxOccurs)) {
			parent.children().each(initSetElement)
			add = $('<button type="button"/>')
			add.addClass("add")
			add.text("»")
			add.click(function(){
				get(parent.attr("data-more"), function(data) {
					var input = $(data)
					add.before(input)
					initElements(input)
					input.each(initSetElement)
				})
			})
			parent.append(add)
		}
		if (minOccurs) {
			var n = parseInt(minOccurs) - count
			for (var i=0; i<n; i++) {
				get(parent.attr("data-more"), function(data) {
					var input = $(data)
					if (add) {
						add.before(input)
					} else {
						parent.append(input)
					}
					initElements(input)
					input.each(initSetElement)
				})
			}
		}
	})
}

function listSearchResults(url, frame, divert) {
	frames[frame].location.href = "about:blank"
	get(url, function(data) {
		if (data) {
			var result = $(data).children()
			var ul = $("<ul/>")
			result.each(function() {
				var li = $("<li/>")
				var link = $("<a/>")
				link.attr("href", $(this).attr("about") + "?pre-view")
				if (divert && window.divertedLinkClicked) {
					link.attr("onclick", "window.parent.divertedLinkClicked(event)")
				}
				link.append($(this).text())
				li.append(link)
				ul.append(li)
			})
			var doc = frames[frame].document
			doc.open()
			doc.write("<ul>" + ul.html() + "</ul>")
			doc.close()
		}
	})
}

function initListElements(form) {
	var lists = $("[data-member]", form)
	lists.each(function() {
		var list = $(this)
		var members = $("[rel='rdf:first'],[rel='rdf:rest']", list)
		members.each(function() {
			var row = $(this)
			if (!row.attr("about") && row.parent().attr("resource")) {
				row.attr("about", row.parent().attr("resource"))
			}
			if (!row.attr("resource") && row.children().attr("about")) {
				row.attr("resource", row.children().attr("about"))
			}
		})
		$("[rel='rdf:first']").each(initListElement)
		list.append(members)
		list.sortable({
				items: "[rel='rdf:first']",
				update: function(event, ui) { updateList(list) }
		})
		var dragover = function(event) {
			if (!list.hasClass("drag-over")) {
				list.addClass("drag-over")
			}
			return stopPropagation(event)
		}
		var dragleave = function(event) {
			list.removeClass("drag-over")
			return stopPropagation(event)
		}
		if (this.addEventListener) {
			this.addEventListener("dragenter", dragover, false)
			this.addEventListener("dragover", dragover, false)
			this.addEventListener("dragleave", dragleave, false)
			this.addEventListener("drop", pasteListURL(dragleave), false)
		} else if (this.attachEvent) {
			this.attachEvent("ondragenter", dragover)
			this.attachEvent("ondragover", dragover)
			this.attachEvent("ondragleave", dragleave)
			this.attachEvent("ondrop", pasteListURL(dragleave))
		}
	})
}

function initListElement() {
	var row = $(this)
	var remove = $('<button type="button"/>')
	remove.addClass("remove")
	remove.text("×")
	remove.click(function(){
		var list = row.parent()
		var rest = list.children("[about='" + row.attr("about") + "'][rel='rdf:rest']")
		row.remove()
		rest.remove()
		updateList(list)
	})
	row.append(remove)
}

function stopPropagation(event) {
	if (event.preventDefault) {
		event.preventDefault()
	}
	return false
}

function pasteListURL(callback) {
	return function(event) {
		var target = event.target ? event.target : event.srcElement
		if (event.preventDefault) {
			event.preventDefault()
		}
		var data = event.originalEvent ? event.originalEvent.dataTransfer : event.dataTransfer
		if (!data) {
			data = event.clipboardData
		}
		if (!data) {
			data = window.clipboardData
		}
		var uris = getIRIs(data.getData('URL'))
		if (!uris.size()) {
			uris = getIRIs(data.getData("Text"))
		}
		var selector = "[data-member]"
		var script = $(target)
		if (!script.is(selector)) {
			$(target).parents().each(function() {
				var s = $(this).children(selector)
				if (s.size()) {
					script = s
				}
			})
		}
		uris.each(function() {
			var uri = this
			if (uri.indexOf('?view') >= 0) {
				uri = uri.substring(0, uri.indexOf('?view'))
			}
			if (uri.indexOf('?pre-view') >= 0) {
				uri = uri.substring(0, uri.indexOf('?pre-view'))
			}
			addListItem(uri, script)
		})
		return callback(event)
	}
}

function pasteURL(callback) {
	return function(event) {
		var target = event.target ? event.target : event.srcElement
		if (event.preventDefault) {
			event.preventDefault()
		}
		var data = event.originalEvent ? event.originalEvent.dataTransfer : event.dataTransfer
		if (!data) {
			data = event.clipboardData
		}
		if (!data) {
			data = window.clipboardData
		}
		var uris = getIRIs(data.getData('URL'))
		if (!uris.size()) {
			uris = getIRIs(data.getData("Text"))
		}
		var selector = "[data-add]"
		var script = $(target)
		if (!script.children(selector).size()) {
			$(target).parents().each(function() {
				if ($(this).is(selector)) {
					script = $(this)
				}
			})
		}
		uris.each(function() {
			var uri = this
			if (uri.indexOf('?view') >= 0) {
				uri = uri.substring(0, uri.indexOf('?view'))
			}
			if (uri.indexOf('?pre-view') >= 0) {
				uri = uri.substring(0, uri.indexOf('?pre-view'))
			}
			addSetItem(uri, script)
		})
		return callback(event)
	}
}

function addSetItem(uri, script) {
	var url = script.attr("data-add").replace("{about}", encodeURIComponent(uri))
	get(url, function(data) {
		var input = data ? $(data) : data
		if (input && input.is("[about='" + uri + "']")) {
			if (script.children("button.add").size()) {
				script.children("button.add").before(input)
			} else {
				script.append(input)
			}
			input.each(initSetElement)
		} else if (window.showError) {
			showError("Invalid Relationship")
		}
	})
}

function getIRIs(iri) {
	var set = iri ? iri.replace(/\s+$/,"").replace(/^\s+/,"").replace(/\s+/,'\n') : ""
	return $(set.split('\n')).filter(function() {
		if (this.indexOf('>') >= 0 || this.indexOf('<') >= 0) {
			return false
		}
		if (this.indexOf(']') >= 0 || this.indexOf('[') >= 0) {
			return false
		}
		if (this.indexOf('\n') >= 0 || this.indexOf('\r') >= 0) {
			return false
		}
		if (this.indexOf(':') < 0 || this.indexOf('_:') >= 0) {
			return false
		}
		return true
	}).map(function() {
		if (this.indexOf('?view') >= 0) {
			return this.substring(0, this.indexOf('?view'))
		}
		if (this.indexOf('?pre-view') >= 0) {
			return this.substring(0, this.indexOf('?pre-view'))
		}
		return this
	})
}

function addListItem(uri, list) {
	var url = list.attr("data-member").replace("{about}", encodeURIComponent(uri))
	get(url, function(data) {
		var input = data ? $(data) : data
		if (input && input.size()) {
			var first = $("<div/>")
			first.attr("about", '[' + $.rdf.blank('[]').value + ']')
			first.attr("rel", "rdf:first")
			first.attr("resource", uri)
			first.append(input)
			list.append(first)
			input.each(initListElement)
			updateList(list)
		} else if (window.showError) {
			showError("Invalid Relationship")
		}
	})
}

function initSetElement() {
	var row = $(this)
	var remove = $('<button type="button"/>')
	remove.addClass("remove")
	remove.text("×")
	remove.click(function(){
		var list = row.parent()
		row.remove()
		remove.remove()
	})
	if (this.tagName.toLowerCase() == "tr") {
		var cell = $("<td/>")
		cell.append(remove)
		row.append(cell)
	} else {
		row.append(remove)
	}
}

function updateList(list) {
	var elements = list.children("[rel='rdf:first']")
	if (elements.size()) {
		list.attr("resource", $(elements.get(0)).attr("about"))
	} else {
		list.attr("resource", rdfnil)
	}
	elements.each(function(i){
		var about = $(this).attr("about")
		if (about) {
			var rest = list.children("[about='" + about + "'][rel='rdf:rest']")
			if (rest.size() && i + 1 < elements.size()) {
				rest.attr("resource", $(elements.get(i + 1)).attr("about"))
			} else if (rest.size()) {
				rest.attr("resource", rdfnil)
			} else {
				var rest = $("<div/>")
				rest.attr("about", about)
				rest.attr("rel", "rdf:rest")
				if (i + 1 < elements.size()) {
					rest.attr("resource", $(elements.get(i + 1)).attr("about"))
				} else {
					rest.attr("resource", rdfnil)
				} 
				list.append(rest)
			}
		}
	})
}

function textContent(node) {
	if (node && node.textContent)
		return node.textContent
	if (node)
		return node.text // IE
	return null
}
