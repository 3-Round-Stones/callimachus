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
package org.callimachusproject.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.client.HttpClient;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.http.object.client.HttpUriEntity;
import org.openrdf.http.object.fluid.FluidBuilder;
import org.openrdf.http.object.fluid.FluidException;
import org.openrdf.http.object.fluid.FluidFactory;

public class TemplateEngine {

	public static TemplateEngine newInstance(HttpClient client) {
		return new TemplateEngine(client);
	}

	private final HttpUriClient client;

	public TemplateEngine(final HttpClient client) {
		if (client instanceof HttpUriClient) {
			this.client = (HttpUriClient) client;
		} else {
			this.client = new HttpUriClient() {
				protected HttpClient getDelegate() {
					return client;
				}
			};
		}
	}

	public Template getTemplate(String systemId) throws IOException,
			TemplateException {
		HttpUriEntity resp = client.getEntity(systemId, "application/xhtml+xml, application/xml, text/xml");
		systemId = resp.getSystemId();
		InputStream in = resp.getContent();
		return getTemplate(in, systemId, null);
	}

	public Template getTemplate(InputStream in, String systemId) throws IOException,
			TemplateException {
		return getTemplate(in, systemId, null);
	}

	public Template getTemplate(InputStream in, String systemId,
			Map<String, ?> parameters) throws IOException,
			TemplateException {
		try {
			return new Template(asXMLEventReader(in, systemId), systemId);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		} catch (TransformerException e) {
			throw new TemplateException(e);
		}
	}

	public Template getTemplate(Reader in, String systemId) throws IOException,
			TemplateException {
		return getTemplate(in, systemId, null);
	}

	public Template getTemplate(Reader in, String systemId,
			Map<String, ?> parameters) throws IOException,
			TemplateException {
		try {
			return new Template(asXMLEventReader(in, systemId), systemId);
		} catch (XMLStreamException e) {
			throw new TemplateException(e);
		} catch (TransformerException e) {
			throw new TemplateException(e);
		}
	}

	private XMLEventReader asXMLEventReader(InputStream in, String systemId)
			throws IOException, TransformerException {
		try {
			FluidBuilder fb = FluidFactory.getInstance().builder();
			return fb.stream(in, systemId, "application/xml").asXMLEventReader();
		} catch (FluidException e) {
			throw new TransformerException(e);
		}
	}

	private XMLEventReader asXMLEventReader(Reader in, String systemId)
			throws IOException, TransformerException {
		try {
			FluidBuilder fb = FluidFactory.getInstance().builder();
			return fb.read(in, systemId, "text/xml").asXMLEventReader();
		} catch (FluidException e) {
			throw new TransformerException(e);
		}
	}
}
