package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.SAXException;

/**
 * Converts (and revalidates) one XML systemId into a {@link DOMSource}.
 * 
 * @author James Leigh
 *
 */
public class DOMSourceResolver extends DocumentObjectResolver<DOMSource, SAXException> {
	private DOMSourceFactory sourceFactory = DOMSourceFactory.newInstance();

	public DOMSourceResolver() {
	}

	protected String[] getContentTypes() {
		return new String[] { "application/xml", "application/xslt+xml",
				"text/xml", "text/xsl" };
	}

	protected boolean isReusable() {
		return true;
	}

	protected DOMSource create(String systemId, Reader in) throws IOException, SAXException {
		try {
			return sourceFactory.createSource(in, systemId);
		} catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}

	protected DOMSource create(String systemId, InputStream in) throws IOException, SAXException {
		try {
			return sourceFactory.createSource(in, systemId);
		} catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}

}
