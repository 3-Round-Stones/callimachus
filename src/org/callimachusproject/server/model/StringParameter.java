/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.server.util.Accepter;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Provides an {@link Parameter} interface for a query parameter or header value.
 */
public class StringParameter implements Parameter {
	private final AbstractFluid reader = AbstractFluid.getInstance();
	private final String[] values;
	private final String base;
	private final ObjectConnection con;
	private final String[] mediaTypes;

	public StringParameter(String[] mediaTypes, String mimeType,
			String[] values, String base, ObjectConnection con) {
		if (mediaTypes == null || mediaTypes.length == 0) {
			this.mediaTypes = new String[] { mimeType };
		} else {
			this.mediaTypes = mediaTypes;
		}
		this.values = values;
		this.base = base;
		this.con = con;
	}

	public Collection<? extends MimeType> getReadableTypes(Class<?> ctype,
			Type gtype, Accepter accepter) throws MimeTypeParseException {
		if (!accepter.isAcceptable(this.mediaTypes))
			return Collections.emptySet();
		MessageType type = new MessageType(null, ctype, gtype, con);
		if (type.is(String.class))
			return accepter.getAcceptable(this.mediaTypes);
		if (type.isSetOrArrayOf(String.class))
			return accepter.getAcceptable(this.mediaTypes);
		List<MimeType> acceptable = new ArrayList<MimeType>();
		for (MimeType m : accepter.getAcceptable(this.mediaTypes)) {
			if (reader.isReadable(type.as(m.toString()))) {
				acceptable.add(m);
			} else if (type.isSetOrArray()) {
				if (reader.isReadable(type.component(m.toString()))) {
					acceptable.add(m);
				}
			}
		}
		return acceptable;
	}

	public <T> T read(Class<T> ctype, Type genericType, String[] mediaTypes)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, MimeTypeParseException,
			URISyntaxException {
		MessageType type = new MessageType(null, ctype, genericType, con);
		if (type.is(String.class)) {
			if (values != null && values.length > 0)
				return (T) type.cast(values[0]);
			return null;
		}
		if (type.isSetOrArrayOf(String.class)) {
			return (T) type.castArray(values);
		}
		Class<?> componentType = type.getComponentClass();
		if (type.isArray() && isReadable(componentType, mediaTypes))
			return (T) type.castArray(readArray(componentType, mediaTypes));
		if (type.isSet() && isReadable(componentType, mediaTypes))
			return (T) type.castSet(readSet(componentType, mediaTypes));
		if (values != null && values.length > 0)
			return read(values[0], ctype, genericType, mediaTypes);
		return null;
	}

	private <T> T[] readArray(Class<T> componentType, String[] mediaTypes)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, MimeTypeParseException,
			URISyntaxException {
		if (values == null)
			return null;
		T[] result = (T[]) Array.newInstance(componentType, values.length);
		for (int i = 0; i < values.length; i++) {
			result[i] = read(values[i], componentType, componentType,
					mediaTypes);
		}
		return result;
	}

	private <T> Set<T> readSet(Class<T> componentType, String[] mediaTypes)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, MimeTypeParseException,
			URISyntaxException {
		Set<T> result = new LinkedHashSet<T>(values.length);
		for (int i = 0; i < values.length; i++) {
			result
					.add(read(values[i], componentType, componentType,
							mediaTypes));
		}
		return result;
	}

	private <T> T read(String value, Class<T> ctype, Type genericType,
			String... mediaTypes) throws TransformerConfigurationException,
			OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException,
			MimeTypeParseException, URISyntaxException {
		String media = getMediaType(ctype, genericType, mediaTypes);
		MessageType type = new MessageType(media, ctype, genericType, con);
		Charset charset = Charset.forName("UTF-16");
		byte[] buf = value.getBytes(charset);
		ReadableByteChannel in = ChannelUtil.newChannel(buf);
		Object result = reader.produce(type, in, charset, base, null);
		return (T) type.cast(result);
	}

	private boolean isReadable(Class<?> componentType, String[] mediaTypes)
			throws MimeTypeParseException {
		String media = getMediaType(componentType, componentType, mediaTypes);
		return reader.isReadable(new MessageType(media, componentType,
				componentType, con));
	}

	private String getMediaType(Class<?> type, Type genericType,
			String[] mediaTypes) throws MimeTypeParseException {
		Accepter accepter = new Accepter(mediaTypes);
		for (MimeType m : accepter.getAcceptable(this.mediaTypes)) {
			if (reader.isReadable(new MessageType(m.toString(), type,
					genericType, con)))
				return m.toString();
		}
		return null;
	}

}
