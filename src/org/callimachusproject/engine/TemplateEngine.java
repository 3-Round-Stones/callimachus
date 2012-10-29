package org.callimachusproject.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpResponse;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;

public class TemplateEngine {

	public static TemplateEngine newInstance() {
		return new TemplateEngine();
	}

	public static TemplateEngine getInstance() {
		return instance;
	}

	private static final TemplateEngine instance = newInstance();

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
			return new Template(asXMLEventReader(in, systemId), systemId);
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
			return new Template(asXMLEventReader(in, systemId), systemId);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		} catch (TransformerException e) {
			throw new TemplateException(e);
		}
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

	private XMLEventReader asXMLEventReader(Reader in, String systemId)
			throws IOException, TransformerException {
		try {
			FluidBuilder fb = FluidFactory.getInstance().builder();
			return fb.read(in, systemId, "text/xml").asXMLEventReader();
		} catch (FluidException e) {
			throw new TransformerException(e);
		}
	}
}
