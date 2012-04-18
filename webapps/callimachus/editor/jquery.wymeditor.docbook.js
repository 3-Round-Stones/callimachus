// jquery.wymeditor.docbook.js

/*
 * Add wym.docbook() function to read html as docbook.
 * Add wym.docbook(xml) function to write docbook as html.
 */
(function($){

function parseXml(urlOrXml, callback, sync) {
	var async = !sync && typeof callback == 'function';
	var ret = this;
	if (typeof callback != 'function') {
		callback = function(data) {
			ret = data;
		};
	}
	if (urlOrXml.indexOf('<') < 0) {
		$.ajax({
			url: urlOrXml,
			async: async,
			beforeSend: withCredentials,
			success: function(xml) {
				if (typeof xml == 'string') {
					parseXml(xml, callback, sync);
				} else {
					callback(xml);
				}
			}
		});
	} else if (window.DOMParser) {
		var parser = new DOMParser();
		callback(parser.parseFromString(urlOrXml, "text/xml"));
	} else if (window.ActiveXObject) {
		var doc = new ActiveXObject("Microsoft.XMLDOM");
		doc.async = async;
		doc.loadXML(urlOrXml);
		callback(doc);
	}
	return ret;
}

function xslt(xslUrl, xmlUrl, callback, sync) {
	var async = !sync && typeof callback == 'function';
	var ret = this;
	if (typeof callback != 'function') {
		callback = function(data) {
			ret = data;
		};
	}
	parseXml(xslUrl, function(xsl) {
		parseXml(xmlUrl, function(xml) {
			if (window.XSLTProcessor) {
				var xsltproc = new XSLTProcessor();
				xsltproc.importStylesheet(xsl);
				var doc = xsltproc.transformToDocument(xml);
				var serializer = new XMLSerializer();
				callback(serializer.serializeToString(doc));
			} else if (window.ActiveXObject) {
				callback(xml.transformNode(xsl));
			}
		}, !async);
	}, !async);
	return ret;
}

function withCredentials(req) {
	try {
		req.withCredentials = true;
	} catch (e) {}
}

WYMeditor.editor.prototype.docbook = function(xml) {
	if (xml && typeof xml == 'string') {
		var xhtml = xslt(calli.getCallimachusUrl("editor/docbook2xhtml.xsl"), xml);
		xhtml = xhtml.match(/<body[^>]*>([\s\S]*)<\/body>/)[1];
		this.html(xhtml);
	} else if (typeof xml == 'string') {
		this.html(xml);
	} else {
		var xhtml = this.xhtml();
		if (typeof xhtml != 'string') return xhtml;
		xhtml = '<html xmlns="http://www.w3.org/1999/xhtml"><head><title></title></head><body>' + xhtml + '</body></html>';
		return xslt(calli.getCallimachusUrl("editor/xhtml2docbook.xsl"), xhtml);
	}
};

})(jQuery);
