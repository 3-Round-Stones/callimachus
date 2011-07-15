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
	var page = '<?xml version="1.0" encoding="UTF-8" ?>\n<?xml-stylesheet type="text/xsl" href="/layout/template.xsl"?>\n<html xmlns="http://www.w3.org/1999/xhtml">\n<head>\n\t<title>' + label + '</title>\n</head>\n<body>\n\t<h1>' + label + '</h1>\n</body>\n</html>';
	var etag = null;

	function outer(node) {
		var html = $($('<div/>').html($(node).clone())).html();
		$(node).remove();
		return html;
	}

	$(document).ready(function() {
		WYMeditor.XhtmlValidator._attributes['core']['attributes'].push('data-dialog', 'data-chart-type','data-chart-source','data-chart-options');
		$('.wym_box_0').wymeditor({
			html: '<h1></h1>',
			lang: null,
			initSkin: false,
			loadSkin: false,
			jQueryPath: '/callimachus/scripts/jquery.js',
			dialogFeatures: 'jQuery.dialog',
			dialogFeaturesPreview: 'jQuery.dialog',
			dialogImageHtml: outer($('.wym_dialog_image')),
			dialogTableHtml: outer($('.wym_dialog_table')),
			dialogPreviewHtml: outer($('.wym_dialog_preview')),
			dialogLinkHtml: outer($('.wym_dialog_link'))
		});
	});

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
								page = xhr.responseText;
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
						window.WYMeditor.INSTANCES[0].initHtml(page);
					}
				}
			});
		} else { // new page
			window.WYMeditor.INSTANCES[0].initHtml(page);
		}
	});

	$('#form').submit(function(event) {
		var xhtml = jQuery.wymeditors(0).xhtml();
		jQuery.getJSON('/callimachus/profile', function(json) {
			try {
				var items = [];
				$.each(json, function(key, val) {
					if (key) {
						items.push('xmlns:' + key + '="' + val + '"');
					}
				});
				var page = xhtml.match(/([\s\S]*<body[^>]*>)([\s\S]*)(<\/body>\s*<\/html>\s*)/);
				var body = page[2];
				var header = page[1].replace(/<html\s+(xmlns[:\w-_.]*="[^"]*"\s*)*/g, '<html ');
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
						var h1 = $(':header:first', $('#wym-iframe')[0].contentWindow.document).text();
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
