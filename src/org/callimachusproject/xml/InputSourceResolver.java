package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class InputSourceResolver extends
		DocumentObjectResolver<InputSource, SAXException> implements
		EntityResolver {

	@Override
	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		return resolve(systemId);
	}

	protected String[] getContentTypes() {
		return new String[] { "application/xml", "application/xslt+xml",
				"text/xml", "text/xsl" };
	}

	protected boolean isReusable() {
		return false;
	}

	protected InputSource create(String systemId, Reader in) {
		InputSource source = new InputSource(in);
		if (systemId != null) {
			source.setSystemId(systemId);
		}
		return source;
	}

	protected InputSource create(String systemId, InputStream in) {
		InputSource source = new InputSource(in);
		if (systemId != null) {
			source.setSystemId(systemId);
		}
		return source;
	}

}