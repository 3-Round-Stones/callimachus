package org.callimachusproject.xslt;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

public class DOMSourceResolver extends DocumentObjectResolver<DOMSource> {
	private DOMSourceFactory sourceFactory = DOMSourceFactory.newInstance();

	public DOMSourceResolver() {
	}

	protected String[] getContentTypes() {
		return new String[] { "application/xml",
				"application/xslt+xml", "text/xml", "text/xsl" };
	}

	protected boolean isReusable() {
		return true;
	}

	protected DOMSource create(String systemId, Reader in) throws TransformerException {
		return sourceFactory.createSource(in, systemId);
	}

	protected DOMSource create(String systemId, InputStream in)
			throws TransformerException {
		return sourceFactory.createSource(in, systemId);
	}


}
