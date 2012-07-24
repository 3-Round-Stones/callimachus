/*
 * Copyright 2009-2010, Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.server.model;

import static java.util.Collections.singleton;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.server.util.Accepter;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.xml.sax.SAXException;

/**
 * Wraps a message response to output to an HTTP response.
 */
public class ResponseParameter implements Parameter {
	private final FluidFactory ff = FluidFactory.getInstance();
	private final Fluid writer;
	private final String[] mimeTypes;
	private final Object result;
	private final Class<?> type;
	private final Type genericType;
	private final Map<String, String> headers = new HashMap<String, String>();
	private final List<String> expects = new ArrayList<String>();

	public ResponseParameter(String[] mimeTypes, Object result, Class<?> type,
			Type genericType, String base, ObjectConnection con) {
		this.result = result;
		this.type = type;
		this.genericType = genericType;
		if (mimeTypes == null || mimeTypes.length < 1) {
			this.mimeTypes = new String[] { "*/*" };
		} else {
			this.mimeTypes = mimeTypes;
		}
		FluidBuilder builder = ff.builder(con);
		Fluid fluid = null;
		for (String media : this.mimeTypes) {
			if (builder.isConsumable(media, type, genericType)) {
				fluid = builder.consume(media, type, genericType, result, base);
				break;
			}
		}
		if (fluid == null)
			throw new ClassCastException(type.getSimpleName()
					+ " cannot be converted into " + Arrays.toString(this.mimeTypes));
		this.writer = fluid;
	}

	public String toString() {
		return String.valueOf(result);
	}

	public HttpEntity asHttpEntity(String media) throws IOException, OpenRDFException, XMLStreamException, TransformerException, ParserConfigurationException {
		return writer.asHttpEntity(media);
	}

	public Collection<? extends MimeType> getReadableTypes(Class<?> type,
			Type genericType, Accepter accepter) throws MimeTypeParseException {
		if (!accepter.isAcceptable(mimeTypes))
			return Collections.emptySet();
		if (this.type.equals(type) && this.genericType.equals(genericType))
			return accepter.getAcceptable();
		List<MimeType> acceptable = new ArrayList<MimeType>();
		for (MimeType mimeType : accepter.getAcceptable(mimeTypes)) {
			if (writer.isProducible(mimeType.toString(), type, genericType)) {
				acceptable.add(mimeType);
			}
		}
		return acceptable;
	}

	public <T> T read(Class<T> type, Type genericType, String[] mediaTypes)
			throws OpenRDFException, TransformerConfigurationException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, MimeTypeParseException,
			URISyntaxException {
		if (this.type.equals(type) && this.genericType.equals(genericType))
			return (T) (result);
		Accepter accepter = new Accepter(mediaTypes);
		for (final MimeType mimeType : accepter.getAcceptable(mimeTypes)) {
			if (writer.isProducible(mimeType.toString(), type, genericType)) {
				return writer.produce(mimeType.toString(), type, genericType);
			}
		}
		throw new ClassCastException(String.valueOf(result)
				+ " cannot be converted into " + type.getSimpleName());
	}

	public boolean isNoContent() {
		return result == null || Set.class.equals(type)
				&& ((Set<?>) result).isEmpty();
	}

	public Set<String> getLocations() {
		if (result instanceof String)
			return singleton((String) result);
		if (result instanceof URI)
			return singleton(((URI) result).stringValue());
		if (result instanceof RDFObject)
			return singleton(((RDFObject) result).getResource().stringValue());
		if (result instanceof Set<?>) {
			if (Set.class.equals(type)) {
				Set<?> set = (Set<?>) result;
				Iterator<?> iter = set.iterator();
				try {
					Set<String> locations = new LinkedHashSet<String>();
					while (iter.hasNext()) {
						Object object = iter.next();
						if (object instanceof RDFObject) {
							locations.add(((RDFObject) object).getResource()
									.stringValue());
						} else if (object instanceof URI) {
							locations.add(((URI) object).stringValue());
						} else if (object instanceof String) {
							locations.add((String) object);
						}
					}
					return locations;
				} finally {
					ObjectConnection.close(iter);
				}
			}
		}
		return null;
	}

	public Map<String, String> getOtherHeaders() {
		return headers;
	}

	public void addHeaders(Map<String, String> map) {
		for (Map.Entry<String, String> e : map.entrySet()) {
			addHeader(e.getKey(), e.getValue());
		}
	}

	public void addHeader(String name, String value) {
		if (headers.containsKey(name)) {
			String string = headers.get(name);
			if (!string.equals(value)) {
				headers.put(name, string + ',' + value);
			}
		} else {
			headers.put(name, value);
		}
	}

	public List<String> getExpects() {
		return expects;
	}

	public void addExpects(String... expects) {
		addExpects(Arrays.asList(expects));
	}

	public void addExpects(List<String> expects) {
		this.expects.addAll(expects);
	}

}
