package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.xml.sax.SAXException;

public class XslTransformer {
	private final XsltExecutable exec;
	private final XdmNodeFactory resolver;
	private final Properties output;

	public XslTransformer(XsltExecutable exec, XdmNodeFactory resolver) {
		this.exec = exec;
		this.resolver = resolver;
		output = exec.getUnderlyingCompiledStylesheet().getOutputProperties();
	}

	public String getMethod() {
		return output.getProperty("method", "xml");
	}

	public String getMediaType() {
		String defaultMediaType = getDefaultMediaType(getMethod());
		return output.getProperty("media-type", defaultMediaType);
	}

	public String getEncoding() {
		return output.getProperty("encoding", "utf-8");
	}

	public void transformToStream(String templateName,
			Map<String, String[]> parameters, OutputStream out)
			throws SAXException, IOException, SaxonApiException {
		XsltTransformer transform = exec.load();
		if (templateName != null) {
			transform.setInitialTemplate(new QName(templateName));
		}
		transform(transform, parameters, out);
	}

	public void transformToStream(InputStream documentStream,
			String documentId, Map<String, String[]> parameters,
			OutputStream out) throws SAXException, IOException,
			SaxonApiException {
		XsltTransformer transform = exec.load();
		transform.setInitialContextNode(resolver.parse(documentId,
				documentStream));
		transform(transform, parameters, out);
	}

	private void transform(XsltTransformer transform,
			Map<String, String[]> parameters, OutputStream out)
			throws SaxonApiException {
		transform.setURIResolver(resolver);
		transform.getUnderlyingController()
				.setUnparsedTextURIResolver(resolver);
		if (parameters != null) {
			for (Map.Entry<String, String[]> e : parameters.entrySet()) {
				QName name = new QName(e.getKey());
				ArrayList<XdmItem> values = new ArrayList<XdmItem>(e.getValue().length);
				for (String value : e.getValue()) {
					values.add(new XdmAtomicValue(value, ItemType.STRING));
				}
				transform.setParameter(name, new XdmValue(values));
			}
		}
		transform.setDestination(new Serializer(out));
		transform.transform();
	}

	private String getDefaultMediaType(String method) {
		if ("xml".equals(method))
			return "application/xml";
		if ("html".equals(method))
			return "text/html";
		return "text/plain";
	}
}
