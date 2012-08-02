package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;

import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

public class XdmNodeResolver extends
		DocumentObjectResolver<XdmNode, SAXException> {
	private final XdmNodeFactory factory;

	public XdmNodeResolver(Processor processor) {
		factory = new XdmNodeFactory(processor);
	}

	public XdmNode parse(String systemId, Reader in) throws SAXException, IOException {
		return factory.parse(systemId, in);
	}

	public XdmNode parse(String systemId, InputStream in) throws SAXException, IOException {
		return factory.parse(systemId, in);
	}

	public EntityResolver getEntityResolver() {
		return factory.getInputSourceResolver();
	}

	@Override
	protected String[] getContentTypes() {
		return factory.getInputSourceResolver().getContentTypes();
	}

	@Override
	protected boolean isReusable() {
		return true;
	}

	@Override
	protected XdmNode create(String systemId, InputStream in)
			throws IOException, SAXException {
		return parse(systemId, in);
	}

	@Override
	protected XdmNode create(String systemId, Reader in) throws IOException,
			SAXException {
		return parse(systemId, in);
	}

}
