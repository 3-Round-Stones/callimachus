package org.callimachusproject.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

import org.callimachusproject.xml.DocumentObjectResolver;
import org.xml.sax.SAXException;

public class TemplatesResolver extends DocumentObjectResolver<Templates, TransformerException> {
	private TransformerFactory delegate = TransformerFactory.newInstance();
	private DOMSourceFactory sourceFactory = DOMSourceFactory.newInstance();

	protected String[] getContentTypes() {
		return new String[] { "application/xslt+xml", "text/xsl",
				"application/xml", "text/xml" };
	}

	protected boolean isReusable() {
		return true;
	}

	protected Templates create(String systemId, Reader in) throws IOException,
			TransformerException {
		ErrorCatcher error = new ErrorCatcher(systemId);
		delegate.setErrorListener(error);
		try {
			Source source = sourceFactory.createSource(in, systemId);
			try {
				return newTemplates(delegate, source);
			} finally {
				in.close();
			}
		} catch (SAXException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} finally {
			if (error.isFatal())
				throw error.getFatalError();
		}
	}

	protected Templates create(String systemId, InputStream in)
			throws IOException, TransformerException {
		ErrorCatcher error = new ErrorCatcher(systemId);
		delegate.setErrorListener(error);
		try {
			Source source = sourceFactory.createSource(in, systemId);
			try {
				return newTemplates(delegate, source);
			} finally {
				in.close();
			}
		} catch (SAXException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} finally {
			if (error.isFatal())
				throw error.getFatalError();
		}
	}

	private synchronized Templates newTemplates(TransformerFactory delegate,
			Source source) throws TransformerConfigurationException,
			IOException {
		final URIResolver resolver = delegate.getURIResolver();
		CloseableURIResolver opened = new CloseableURIResolver(resolver);
		delegate.setURIResolver(opened);
		try {
			return delegate.newTemplates(source);
		} finally {
			opened.close();
			delegate.setURIResolver(resolver);
		}
	}

}
