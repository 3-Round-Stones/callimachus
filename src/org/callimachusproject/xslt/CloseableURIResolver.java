package org.callimachusproject.xslt;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

public final class CloseableURIResolver implements URIResolver, Closeable {
	private final URIResolver resolver;
	private final List<Closeable> opened = new ArrayList<Closeable>();;

	public CloseableURIResolver(URIResolver resolver) {
		this.resolver = resolver;
	}

	public Source resolve(String href, String base) throws TransformerException {
		Source source = resolver.resolve(href, base);
		if (source instanceof StreamSource) {
			InputStream in = ((StreamSource) source).getInputStream();
			if (in != null) {
				synchronized (opened) {
					opened.add(in);
				}
			}
			Reader reader = ((StreamSource) source).getReader();
			if (reader != null) {
				synchronized (opened) {
					opened.add(reader);
				}
			}
		}
		return source;
	}

	@Override
	public void close() throws IOException {
		synchronized (opened) {
			for (Closeable closeable : opened) {
				closeable.close();
			}
			opened.clear();
		}
	}
}