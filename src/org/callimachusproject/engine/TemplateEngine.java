package org.callimachusproject.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.client.HttpOriginClient;
import org.callimachusproject.client.HttpUriEntity;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;

public class TemplateEngine {

	public static TemplateEngine newInstance(String origin) {
		return new TemplateEngine(origin);
	}

	private final HttpOriginClient client;

	public TemplateEngine(String origin) {
		this.client = new HttpOriginClient(origin);
	}

	public Template getTemplate(String systemId) throws IOException,
			TemplateException {
		HttpUriEntity resp = client.getEntity(systemId, "appliaction/xhtml+xml, application/xml, text/xml");
		systemId = resp.getSystemId();
		InputStream in = resp.getContent();
		return getTemplate(in, systemId, null);
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
