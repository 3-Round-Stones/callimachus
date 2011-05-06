// page-edit.js

jQuery(function($){
	var url = $('body').attr('about');
	var etag = $('meta[http-equiv="etag"]').attr('content');
	if (location.hash && location.hash.indexOf('#!') == 0) { // new page
		url = location.hash.substring(2);
		etag = null;
	} else if (location.search == "?create") { // new page
		url = '';
		etag = null;
	}
	var local = url.replace(/.*\//, '').replace(/\.[a-zA-Z]+$/, '');
	var label = decodeURIComponent(local);
	if (!label) {
		label = 'Enter Title Here';
	}
	var page = [null, '<?xml version="1.0" encoding="UTF-8" ?><?xml-stylesheet type="text/xsl" href="/layout/template.xsl"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>' + label + '</title></head><body about="?this">', '<h1>' + label + '</h1>','</body></html>'];

	function outter(node) {
		var html = $($('<div/>').html($(node).clone())).html();
		$(node).remove();
		return html;
	}

	function inlineProperties(html) {
		var result = html;
		result = result.replace(/<input\b(\s*[^>]*)\bproperty="(\w*:\w*)"(\s*[^>]*>)/g, '<input$1value="{$2}"$3');
		result = result.replace(/<(\w*)\b(\s*[^>]*)\bproperty="(\w*:\w*)"(\s*[^>]*)(?:\/>|>\s*<\/\1>)/g, '<$1$2$4>{$3}</$1>');
		result = result.replace(/<span\s+property="(\w*:\w*)"\s*\/>/g, '{$1}');
		return result;
	}

	function outlineProperties(html) {
		var result = html;
		result = result.replace(/<input\b(\s*[^>]*)\value="{(\w*:\w*)}"(\s*[^>]*>)/g, '<input$1property="$2"$3');
		result = result.replace(/<pre(\s*[^>]*)>{(\w*:\w*)}<\/pre>/g, '<pre$1 property="$2" />');
		result = result.replace(/<(\w*)\s*>{(\w*:\w*)}<\/\1>/g, '<$1 property="$2" />');
		result = result.replace(/{(\w*:\w*)}/g, '<span property="$1" />');
		return result;
	}

	function initHtml(html) {
		jQuery.wymeditors(0)._html = inlineProperties(html);
		window.WYMeditor.INSTANCES[0].initIframe($('#iframe')[0]);
		$('#iframe').trigger('load');
		setTimeout(function(){
			if (!$('body', $('#iframe')[0].contentWindow.document).children().length) {
				// IE need this called twice on first load (reload doesn't seem to work)
				window.WYMeditor.INSTANCES[0].initIframe($('#iframe')[0]);
			}
		}, 1000);
		var wym = window.WYMeditor.INSTANCES[0];
		var previously = wym.exec;
		var dialog_rel = outter($('.wym_dialog_rel'));
		function insertDiv(styleClass) {
			var block = jQuery(wym.findUp(wym.container(), WYMeditor.BLOCKS)).get(0);
			var node = jQuery(wym.findUp(wym.container(), WYMeditor.MAIN_CONTAINERS)).get(0);
			if((!node || !node.parentNode) && block && block.parentNode) {
				var div = $('<div class="' + styleClass + '"><p></p></div>');
				jQuery(block).append(div);
				wym.setFocusToNode(div.children()[0]);
			} else if(!node || !node.parentNode) {
				var div = $('<div class="' + styleClass + '"><p></p></div>');
				jQuery(wym._doc.body).append(div);
				wym.setFocusToNode(div.children()[0]);
			} else if ($(node.parentNode).is('div.' + styleClass) && $(node.parentNode).children().length < 2) {
				jQuery(node.parentNode).replaceWith(node);
				wym.setFocusToNode(node);
			} else if ($(node.parentNode).is('div.' + styleClass)) {
				jQuery(node.parentNode).after(node);
				wym.setFocusToNode(node);
			} else {
				var div = $('<div class="' + styleClass + '"></div>');
				div.append($(node).clone());
				jQuery(node).replaceWith(div);
				wym.setFocusToNode(div.children()[0]);
			}
		}
		wym.exec = function(cmd) {
			switch (cmd) {
			case 'InsertVBox':
				insertDiv('vbox');
			break;
			case 'InsertHBox':
				insertDiv('hbox');
			break;
			case 'InsertRel':
				this.dialog('InsertRel', null, dialog_rel);
				var blocks = new Array("address", "div", "dl", "fieldset", "form", "noscript", "ol", "ul", "dd", "dt", "li", "tr");
				var node = jQuery(wym.findUp(wym.container(), blocks)).get(0);
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
					}
					if (!variable || variable.indexOf('?') != 0) {
						$(this).trigger('calliError', 'Variables must start with "?"');
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
							wym.setFocusToNode(div.children()[0]);
						} else {
							var div = '<div ' + attr + '="' + curie + '" resource="' + variable + '"><p>{rdfs:label}</p></div>';
							jQuery(wym._doc.body).append(div);
							wym.setFocusToNode(div.children()[0]);
						}
					}
					if (window.opener) {
						window.close();
					} else if (window.dialog) {
						window.dialog.dialog('close');
					}
					return false;
				});
			break;
			default:
				return previously.call(this, cmd);
			}
		}
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
			html: inlineProperties(page[2]),
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

	$(window).one('load', function() {
		if (etag) {
			jQuery.ajax({
				type: "GET", url: url,
				beforeSend: function(xhr) {
					xhr.setRequestHeader('Cache-Control', 'no-store');
				},
				complete: function(xhr) {
					if (xhr.readyState == 4) {
						if (xhr.status == 200) {
							var type = xhr.getResponseHeader("Content-Type");
							if (type && type.indexOf('application/xhtml+xml') == 0) {
								etag = xhr.getResponseHeader("ETag");
								page = xhr.responseText.match(/([\s\S]*<body[^>]*>)([\s\S]*)(<\/body>\s*<\/html>\s*)/);
								$('#create').hide();
								$('#abort').hide();
								$('#save').show();
								$('#cancel').show();
								$('#delete').show();
							}
						} else {
							etag = null;
							url = null;
						}
						initHtml(page[2]);
					}
				}
			});
		} else { // new page
			initHtml(page[2]);
		}
	});

	$('#form').submit(function(event) {
		var body = outlineProperties(jQuery.wymeditors(0).xhtml());
		jQuery.getJSON('/callimachus/profile', function(json) {
			try {
				var items = [];
				$.each(json, function(key, val) {
					if (key) {
						items.push('xmlns:' + key + '="' + val + '"');
					}
				});
				var header = page[1].replace(/<html\s+(xmlns[:\w]*="[^"]*"\s*)*/g, '<html ');
				header = header.replace(/<html\s*/, '<html xmlns="http://www.w3.org/1999/xhtml" ' + items.join(' ') + ' ');
				var m = body.match(/<h(\d)(\s*[^>]*>(?:[^<]|<[^h\/]|<\/[^h])*)<\/h\1>/);
				if (m) {
					header = header.replace(/<title\b[\s\S]*<\/title>/, '<title about="?this"' + m[2] + '</title>');
					header = header.replace(/<title\b[^>]*\/>/, '<title about="?this"' + m[2] + '</title>');
				}
				m = body.match(/<h(\d)(\s*[^>]*)\/>/);
				if (m) {
					header = header.replace(/<title\b[\s\S]*<\/title>/, '<title about="?this"' + m[2] + '/>');
					header = header.replace(/<title\b[^>]*\/>/, '<title about="?this"' + m[2] + '/>');
				}
				var html = header + body + page[3];
				if (etag) {
					jQuery.ajax({
						type: "PUT", url: url,
						beforeSend: function(xhr) {
							xhr.setRequestHeader('If-Match', etag);
						},
						contentType: 'application/xhtml+xml',
						data: html,
						success: function() {
							location.replace(url + "?view");
						}
					});
				} else { // new page
					if (!url) {
						// no URL provided
						var h1 = $(':header:first', $('#iframe')[0].contentWindow.document).text();
						if (h1) {
							h1 = h1.replace(/^\s+/, '').replace(/\s+$/, '').replace(/\s+/, ' ');
						} else {
							h1 = 'Enter Page Title Here';
						}
						h1 = prompt('Please provide a page title', h1);
						if (!h1 || h1 == 'Enter Page Title Here') throw 'No page title provided';
						html = html.replace(/<title>[^<]*<\/title>/, '<title>' + h1 + '</title>');
						url = 'http://localhost:8080/page/' + encodeURI(h1).replace(/%20/g,'+') + '.xhtml';
					}
					jQuery.ajax({
						type: "POST", url: location.href,
						beforeSend: function(xhr) {
							xhr.setRequestHeader('Location', url);
						},
						contentType: 'application/xhtml+xml',
						data: html,
						success: function() {
							location.replace(url + "?view");
						}
					});
				}
			} catch (e) {
				$('#form').trigger("calliError", e.description ? e.description : e);
			}
		});
		event.preventDefault();
		return false;
	});

	$(document).bind('calliCreate', function(event) {
		if (event.rdfType == "foaf:Image") {
			jQuery.wymeditors(0).insert("<img src='" + event.about + "' />");
			if (window.dialog) {
				window.dialog.dialog('close');
			}
		}
	});
});
