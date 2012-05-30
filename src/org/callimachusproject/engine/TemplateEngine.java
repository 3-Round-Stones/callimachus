package org.callimachusproject.engine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.ResponseException;
import org.openrdf.repository.RepositoryConnection;
import org.callimachusproject.xslt.TransformBuilder;
import org.callimachusproject.xslt.XSLTransformer;

public class TemplateEngine {
	private static final int XML_BUF = 2048;
	private static final String XSS = "<?xml-stylesheet";
	private static final Pattern TYPE_XSLT = Pattern
			.compile("\\btype=[\"'](text/xsl|application/xslt+xml)[\"']");
	private static final Pattern HREF_XSLT = Pattern
			.compile("<?xml-stylesheet\\b[^>]*\\bhref=[\"']([^\"']*)[\"']");
	private final RepositoryConnection con;
	private final Map<String, Reference<XSLTransformer>> transformers;

	protected TemplateEngine(RepositoryConnection con,
			Map<String, Reference<XSLTransformer>> transformers) {
		this.con = con;
		this.transformers = transformers;
	}

	public Template getTemplate(String url) throws IOException,
			TemplateException {
		return getTemplate(url, null);
	}

	public Template getTemplate(String systemId, Map<String, ?> parameters)
			throws IOException, TemplateException {
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse response = null;
		String url = null;
		String redirect = systemId;
		for (int i = 0; redirect != null && i < 20; i++) {
			url = redirect;
			HttpRequest request = new BasicHttpRequest("GET", redirect);
			request.setHeader("Accept",
					"appliaction/xhtml+xml, application/xml, text/xml");
			response = client.service(request);
			redirect = client.redirectLocation(redirect, response);
		}
		if (response.getStatusLine().getStatusCode() >= 300)
			throw ResponseException.create(response, url);
		InputStream in = response.getEntity().getContent();
		return getTemplate(in, url, parameters);
	}

	public Template getTemplate(InputStream in, String systemId) throws IOException,
			TemplateException {
		return getTemplate(in, systemId, null);
	}

	public Template getTemplate(InputStream in, String systemId,
			Map<String, ?> parameters) throws IOException,
			TemplateException {
		try {
			return new Template(xslt(in, systemId, parameters), systemId, con);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		} catch (TransformerException e) {
			throw new TemplateException(e);
		}
	}

	private XMLEventReader xslt(InputStream in, String systemId,
			Map<String, ?> parameters) throws XMLStreamException,
			IOException, TransformerException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		String href = readXSLTSource(in);
		if (href == null)
			return new XSLTransformer().transform(in, systemId).asXMLEventReader();
		String xsl = URI.create(systemId).resolve(href).toASCIIString();
		XSLTransformer xslt = newXSLTransformer(xsl);
		TransformBuilder transform = xslt.transform(in, systemId);
		if (parameters == null || !parameters.containsKey("systemId")) {
			transform = transform.with("systemId", systemId);
		}
		if (parameters == null || !parameters.containsKey("xsltId")) {
			transform = transform.with("xsltId", xslt.getSystemId());
		}
		if (parameters != null) {
			for (Map.Entry<String, ?> e : parameters.entrySet()) {
				transform = transform.with(e.getKey(), e.getValue());
			}
		}
		return transform.asXMLEventReader();
	}

	private XSLTransformer newXSLTransformer(String xsl) {
		synchronized (transformers) {
			Reference<XSLTransformer> ref = transformers.get(xsl);
			if (ref != null) {
				XSLTransformer xslt = ref.get();
				if (xslt != null)
					return xslt;
			}
			XSLTransformer xslt = new XSLTransformer(xsl);
			transformers.put(xsl, new SoftReference<XSLTransformer>(xslt));
			return xslt;
		}
	}

	/**
	 * This method parses the XSLT processing instruction. JAXP 1.4 (JDK6)
	 * cannot parse processing instructions.
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6849942
	 */
	private String readXSLTSource(InputStream in) throws IOException {
		String instructions = readInstructions(in);
		if (instructions.contains("<?xml-stylesheet ")) {
			if (TYPE_XSLT.matcher(instructions).find()) {
				Matcher matcher = HREF_XSLT.matcher(instructions);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
		}
		return null;
	}

	private String readInstructions(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		in.mark(XML_BUF * 2);
		try {
			Reader reader = new InputStreamReader(in);
			int chr;
			boolean stylesheet = false;
			while ((chr = reader.read()) != -1 && sb.length() < XML_BUF) {
				sb.append((char) chr);
				int l = sb.length();
				if (Character.isWhitespace((char) chr) && sb.length() > XSS.length()) {
					String endsWith = sb.substring(l - 1 - XSS.length(), l - 1);
					if (endsWith.equals(XSS)) {
						stylesheet = true;
					}
				} else if (stylesheet && ((char) chr) == '>') {
					break;
				} else if (((char) chr) != '?' && sb.length() > 1
						&& sb.charAt(sb.length() - 2) == '<') {
					break;
				}
			}
		} finally {
			in.reset();
		}
		return sb.toString();
	}
}
