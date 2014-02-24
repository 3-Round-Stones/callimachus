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

import javax.xml.transform.stream.StreamSource;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Force any open {@link StreamSource}; inteded to be used within an operation.
 * 
 * @author James Leigh
 * 
 */
public final class CloseableEntityResolver implements EntityResolver, Closeable {
	private final EntityResolver resolver;
	private final List<Closeable> opened = new ArrayList<Closeable>();;

	public CloseableEntityResolver(EntityResolver resolver) {
		this.resolver = resolver;
	}

	public InputSource resolveEntity(String publicId, String systemId)
			throws IOException, SAXException {
		InputSource isource = resolver.resolveEntity(publicId, systemId);
		if (isource instanceof Closeable) {
			closeLater((Closeable) isource);
		} else if (isource != null) {
			InputStream in = isource.getByteStream();
			if (in != null) {
				closeLater(in);
			}
			Reader reader = isource.getCharacterStream();
			if (reader != null) {
				closeLater(reader);
			}
		}
		return isource;
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
