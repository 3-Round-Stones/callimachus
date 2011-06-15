// page-edit.js

jQuery(function($){

function getPageLocationURL() {
	// window.location.href needlessly decodes URI-encoded characters in the URI path
	// https://bugs.webkit.org/show_bug.cgi?id=30225
	var path = location.pathname;
	if (path.match(/#/))
		return location.href.replace(path, path.replace('#', '%23'));
	return location.href;
}

	var url = $('body').attr('about');
	if (location.hash && location.hash.indexOf('#!') == 0) { // new page
		url = location.hash.substring(2);
	} else if (location.search == "?create") { // new page
		url = '';
	}
	var local = url.replace(/.*\//, '').replace(/\.[a-zA-Z]+$/, '');
	var label = decodeURIComponent(local);
	if (!label) {
		label = 'Enter Title Here';
	}
	var page = [null, '<?xml version="1.0" encoding="UTF-8" ?><?xml-stylesheet type="text/xsl" href="/layout/template.xsl"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>' + label + '</title></head><body about="?this">', '<h1>' + label + '</h1>','</body></html>'];
	var etag = null;

	function inlineProperties(html) {
		var result = html;
		result = result.replace(/<span\s+property="(\w*:\w*)"\s*\/>/g, '{$1}');
		result = result.replace(/<(\w*)(\s+class="[^"]*")?\s+property="(\w*:\w*)"\s*(?:\/>|>\s*<\/\1>)/g, '<$1$2>{$3}</$1>');
		return result;
	}

	function outlineProperties(html) {
		var result = html;
		result = result.replace(/<(\w*)(\s+class="[^"]*")?\s*>{(\w*:\w*)}<\/\1>/g, '<$1$2 property="$3" />');
		var regex = /{(\w*:\w*)}([^>]*<\/?)(a|abbr|acronym|address|applet|audio|b|bdi|bdo|big|blockquote|body|br|button|canvas|caption|center|cite|code|command|datalist|dd|del|dfn|div|dt|em|embed|fieldset|font|form|h1|h2|h3|h4|h5|h6|i|iframe|img|input|ins|kbd|keygen|label|legend|li|mark|math|meter|noframes|noscript|object|output|p|progress|q|ruby|s|samp|select|small|span|strike|strong|sub|sup|svg|td|th|time|tt|u|var|video|wbr)\b/g;
		var inline = result.replace(regex, '<span property="$1" />$2$3');
		while (result != inline) {
			result = inline;
			inline = result.replace(regex, '<span property="$1" />$2$3');
		}
		return result;
	}

	$(window).one('load', function() {
		if (location.search != "?create") {
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
						window.WYMeditor.INSTANCES[0].initHtml(inlineProperties(page[2]));
					}
				}
			});
		} else { // new page
			window.WYMeditor.INSTANCES[0].initHtml(inlineProperties(page[2]));
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
				var header = page[1].replace(/<html\s+(xmlns[:\w-_]*="[^"]*"\s*)*/g, '<html ');
				header = header.replace(/<html\s*/, '<html xmlns="http://www.w3.org/1999/xhtml" ' + items.join(' ') + ' ');
				var m = body.match(/<h1\s*(?:[^<\/]|<[^\/h]|\/[^>])*>\s*([^<]*)\s*<\//);
				if (m) {
					header = header.replace(/<title\b[\s\S]*<\/title>/, '<title>' + m[1] + '</title>');
					header = header.replace(/<title\b[^>]*\/>/, '<title>' + m[1] + '</title>');
				} else {
					m = body.match(/<h1\s*(?:[^<\/]|<[^\/h]|\/[^>])*\bproperty=['"](\w+:\w+)["']/);
					if (m) {
						header = header.replace(/<title\b[\s\S]*<\/title>/, '<title about="?this" property="' + m[1] + '" />');
						header = header.replace(/<title\b[^>]*\/>/, '<title about="?this" property="' + m[1] + '" />');
					}
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
						type: "POST", url: getPageLocationURL(),
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
