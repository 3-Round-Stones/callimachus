package org.callimachusproject.engine;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpResponse;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.xslt.TransformBuilder;
import org.callimachusproject.xslt.XSLTransformer;
import org.callimachusproject.xslt.XSLTransformerFactory;

public class TemplateEngine {

	public static TemplateEngine newInstance() {
		return new TemplateEngine();
	}

	public static TemplateEngine getInstance() {
		return instance;
	}

	private static final TemplateEngine instance = newInstance();
	private static final int XML_BUF = 2048;
	private static final String XSS = "<?xml-stylesheet";
	private static final Pattern TYPE_XSLT = Pattern
			.compile("\\btype=[\"'](text/xsl|application/xslt+xml)[\"']");
	private static final Pattern HREF_XSLT = Pattern
			.compile("<?xml-stylesheet\\b[^>]*\\bhref=[\"']([^\"']*)[\"']");

	private final Map<String, Reference<XSLTransformer>> transformers = new LinkedHashMap<String, Reference<XSLTransformer>>(
			16, 0.75f, true) {
		private static final long serialVersionUID = 1362917757653811798L;

		protected boolean removeEldestEntry(
				Map.Entry<String, Reference<XSLTransformer>> eldest) {
			return size() > 16;
		}
	};

	public Template getTemplate(String url) throws IOException,
			TemplateException {
		return getTemplate(url, null);
	}

	public Template getTemplate(String systemId, Map<String, ?> parameters)
			throws IOException, TemplateException {
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpResponse resp = client.get(systemId, "appliaction/xhtml+xml, application/xml, text/xml");
		systemId = resp.getLastHeader("Content-Location").getValue();
		InputStream in = resp.getEntity().getContent();
		return getTemplate(in, systemId, parameters);
	}

	public Template getTemplate(InputStream in, String systemId) throws IOException,
			TemplateException {
		return getTemplate(in, systemId, null);
	}

	public Template getTemplate(InputStream in, String systemId,
			Map<String, ?> parameters) throws IOException,
			TemplateException {
		try {
			return new Template(xslt(in, systemId, parameters), systemId);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		} catch (TransformerException e) {
			throw new TemplateException(e);
		}
	}

	public Template getTemplate(Reader in, String systemId) throws IOException,
			TemplateException {
		return getTemplate(in, systemId, null);
	}

	public Template getTemplate(Reader in, String systemId,
			Map<String, ?> parameters) throws IOException,
			TemplateException {
		try {
			return new Template(xslt(in, systemId, parameters), systemId);
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
		in.mark(XML_BUF * 2);
		String href = readXSLTSource(new InputStreamReader(in));
		in.reset();
		if (href == null)
			return asXMLEventReader(in, systemId);
		String xsl = URI.create(systemId).resolve(href).toASCIIString();
		XSLTransformer xslt = newXSLTransformer(xsl);
		TransformBuilder transform = xslt.transform(in, systemId);
		return asXMLEventReader(transform, systemId, parameters, xslt);
	}

	private XMLEventReader asXMLEventReader(InputStream in, String systemId)
			throws IOException, TransformerException {
		try {
			FluidBuilder fb = FluidFactory.getInstance().builder();
			return fb.stream(in, systemId, "application/xml").asXMLEventReader();
		} catch (FluidException e) {
			throw new TransformerException(e);
		}
	}

	private XMLEventReader xslt(Reader in, String systemId,
			Map<String, ?> parameters) throws XMLStreamException,
			IOException, TransformerException {
		if (!in.markSupported()) {
			in = new BufferedReader(in);
		}
		in.mark(XML_BUF);
		String href = readXSLTSource(in);
		in.reset();
		if (href == null)
			return asXMLEventReader(in, systemId);
		String xsl = URI.create(systemId).resolve(href).toASCIIString();
		XSLTransformer xslt = newXSLTransformer(xsl);
		TransformBuilder transform = xslt.transform(in, systemId);
		return asXMLEventReader(transform, systemId, parameters, xslt);
	}

	private XMLEventReader asXMLEventReader(Reader in, String systemId)
			throws IOException, TransformerException {
		try {
			FluidBuilder fb = FluidFactory.getInstance().builder();
			return fb.read(in, systemId, "text/xml").asXMLEventReader();
		} catch (FluidException e) {
			throw new TransformerException(e);
		}
	}

	private XMLEventReader asXMLEventReader(TransformBuilder transform,
			String systemId, Map<String, ?> parameters, XSLTransformer xslt)
			throws TransformerException, IOException {
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
			XSLTransformer xslt = XSLTransformerFactory.getInstance().createTransformer(xsl);
			transformers.put(xsl, new SoftReference<XSLTransformer>(xslt));
			return xslt;
		}
	}

	/**
	 * This method parses the XSLT processing instruction. JAXP 1.4 (JDK6)
	 * cannot parse processing instructions.
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6849942
	 */
	private String readXSLTSource(Reader reader) throws IOException {
		String instructions = readInstructions(reader);
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

	private String readInstructions(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
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
		return sb.toString();
	}
}
