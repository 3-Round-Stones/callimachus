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
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
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

import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.server.util.Accepter;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.xml.sax.SAXException;

/**
 * Wraps a message response to output to an HTTP response.
 */
public class ResponseParameter implements Parameter {
	private final FluidFactory writer = FluidFactory.getInstance();
	private final AbstractFluid reader = AbstractFluid.getInstance();
	private final String[] mimeTypes;
	private final Object result;
	private final Class<?> type;
	private final Type genericType;
	private final String base;
	private final ObjectConnection con;
	private final Map<String, String> headers = new HashMap<String, String>();
	private final List<String> expects = new ArrayList<String>();

	public ResponseParameter(String[] mimeTypes, Object result, Class<?> type,
			Type genericType, String base, ObjectConnection con) {
		this.result = result;
		this.type = type;
		this.genericType = genericType;
		this.base = base;
		this.con = con;
		if (mimeTypes == null || mimeTypes.length < 1) {
			this.mimeTypes = new String[] { "*/*" };
		} else {
			this.mimeTypes = mimeTypes;
		}
	}

	public String toString() {
		return String.valueOf(result);
	}

	public Object getEntity() {
		return result;
	}

	public Collection<? extends MimeType> getReadableTypes(Class<?> type,
			Type genericType, Accepter accepter) throws MimeTypeParseException {
		if (!accepter.isAcceptable(mimeTypes))
			return Collections.emptySet();
		if (this.type.equals(type) && this.genericType.equals(genericType))
			return accepter.getAcceptable();
		List<MimeType> acceptable = new ArrayList<MimeType>();
		for (MimeType mimeType : accepter.getAcceptable(mimeTypes)) {
			if (isWriteable(mimeType.toString())) {
				String contentType = getContentType(mimeType.toString());
				String mime = removeParamaters(contentType);
				if (isReadable(type, genericType, mime)) {
					acceptable.add(mimeType);
				}
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
			if (isWriteable(mimeType.toString())) {
				String contentType = getContentType(mimeType.toString());
				String mime = removeParamaters(contentType);
				Charset charset = getCharset(contentType);
				if (isReadable(type, genericType, mime)) {
					ReadableByteChannel in = write(mimeType.toString(), null).asChannel();
					return (T) (readFrom(type, genericType, mime, charset, in));
				}
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

	public long getSize(String mimeType, Charset charset) {
		return write(mimeType, charset).getByteStreamSize();
	}

	public Fluid write(String mimeType, Charset charset) {
		return writer.consume(new MessageType(mimeType, type, genericType, con),
				result, base, charset);
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

	private boolean isReadable(Class<?> type, Type genericType, String mime) {
		return reader.isReadable(new MessageType(mime, type, genericType, con));
	}

	private boolean isWriteable(String mimeType) {
		return writer.isWriteable(new MessageType(mimeType, type, genericType,
				con));
	}

	private <T> Object readFrom(Class<T> type, Type genericType, String mime,
			Charset charset, ReadableByteChannel in)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, URISyntaxException {
		return reader.produce(new MessageType(mime, type, genericType, con),
				in, charset, base, null);
	}

	private String getContentType(String mimeType) {
		return write(mimeType, null).getContentType();
	}

	private String removeParamaters(String mediaType) {
		if (mediaType == null)
			return null;
		int idx = mediaType.indexOf(';');
		if (idx > 0)
			return mediaType.substring(0, idx);
		return mediaType;
	}

	private Charset getCharset(String mediaType) throws MimeTypeParseException {
		if (mediaType == null)
			return null;
		MimeType m = new MimeType(mediaType);
		String name = m.getParameters().get("charset");
		if (name == null)
			return null;
		return Charset.forName(name);
	}

}
