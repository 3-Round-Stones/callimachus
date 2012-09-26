package org.callimachusproject.xml;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

/**
 * Force any open {@link StreamSource}; inteded to be used within an operation.
 * 
 * @author James Leigh
 *
 */
public final class CloseableURIResolver implements URIResolver, Closeable {
	private final URIResolver resolver;
	private final List<Closeable> opened = new ArrayList<Closeable>();;

	public CloseableURIResolver(URIResolver resolver) {
		this.resolver = resolver;
	}

	public Source resolve(String href, String base) throws TransformerException {
		Source source = resolver.resolve(href, base);
		if (source instanceof Closeable) {
			closeLater((Closeable) source);
		} else if (source instanceof StreamSource) {
			InputStream in = ((StreamSource) source).getInputStream();
			if (in != null) {
				closeLater(in);
			}
			Reader reader = ((StreamSource) source).getReader();
			if (reader != null) {
				closeLater(reader);
			}
		} else if (source instanceof SAXSource) {
			InputSource isource = ((SAXSource) source).getInputSource();
			if (isource != null) {
				InputStream in = isource.getByteStream();
				if (in != null) {
					closeLater(in);
				}
				Reader reader = isource.getCharacterStream();
				if (reader != null) {
					closeLater(reader);
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

	private void closeLater(Closeable in) {
		synchronized (opened) {
			opened.add(in);
		}
	}
}