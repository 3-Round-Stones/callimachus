package org.callimachusproject.xslt;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public class TemplatesResolver extends DocumentObjectResolver<Templates> {
	private TransformerFactory delegate = TransformerFactory.newInstance();
	private DOMSourceFactory sourceFactory = DOMSourceFactory.newInstance();

	protected String[] getContentTypes() {
		return new String[] { "application/xslt+xml", "text/xsl",
				"application/xml", "text/xml" };
	}

	protected boolean isReusable() {
		return true;
	}

	protected Templates create(String systemId, Reader in)
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
		} finally {
			if (error.isFatal())
				throw error.getFatalError();
		}
	}

	private synchronized Templates newTemplates(TransformerFactory delegate,
			Source source) throws TransformerConfigurationException,
			IOException {
		final List<Closeable> opened = new ArrayList<Closeable>();
		final URIResolver resolver = delegate.getURIResolver();
		delegate.setURIResolver(new URIResolver() {
			public Source resolve(String href, String base)
					throws TransformerException {
				Source source = resolver.resolve(href, base);
				if (source instanceof StreamSource) {
					InputStream in = ((StreamSource) source).getInputStream();
					if (in != null) {
						synchronized (opened) {
							opened.add(in);
						}
					}
				}
				return source;
			}
		});
		try {
			return delegate.newTemplates(source);
		} finally {
			for (Closeable closeable : opened) {
				closeable.close();
			}
			delegate.setURIResolver(resolver);
		}
	}

}
