/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
