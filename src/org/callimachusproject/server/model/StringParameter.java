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

import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.Accepter;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Provides an {@link Parameter} interface for a query parameter or header value.
 */
public class StringParameter implements Parameter {
	private final FluidFactory ff = FluidFactory.getInstance();
	private final Fluid sample;
	private final Fluid[] values;
	private final String[] mediaTypes;

	public StringParameter(String[] mediaTypes, String mimeType,
			String[] values, String base, ObjectConnection con) {
		if (mediaTypes == null || mediaTypes.length == 0) {
			this.mediaTypes = new String[] { mimeType };
		} else {
			this.mediaTypes = mediaTypes;
		}
		FluidBuilder fb = ff.builder(con);
		if (values == null || values.length == 0) {
			this.values = new Fluid[0];
			this.sample = fb.media("text/plain");
		} else {
			Charset charset = Charset.forName("UTF-8");
			this.values = new Fluid[values.length];
			for (int i=0; i<values.length; i++) {
				this.values[i] = fb.channel("text/plain;charset=" + charset.name(), ChannelUtil.newChannel(values[i].getBytes(charset)), base);
			}
			this.sample = this.values[0];
		}
	}

	public Collection<? extends MimeType> getReadableTypes(Class<?> ctype,
			Type gtype, Accepter accepter) throws MimeTypeParseException {
		if (!accepter.isAcceptable(this.mediaTypes))
			return Collections.emptySet();
		FluidType type = new FluidType(gtype, null);
		if (type.is(String.class))
			return accepter.getAcceptable(this.mediaTypes);
		if (type.isSetOrArrayOf(String.class))
			return accepter.getAcceptable(this.mediaTypes);
		List<MimeType> acceptable = new ArrayList<MimeType>();
		for (MimeType m : accepter.getAcceptable(this.mediaTypes)) {
			if (sample.toMedia(gtype, m.toString()) != null) {
				acceptable.add(m);
			} else if (type.isSetOrArray()) {
				if (sample.toMedia(type.component(m.toString())) != null) {
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
		FluidType type = new FluidType(genericType, null);
		if (type.is(String.class)) {
			if (values != null && values.length > 0)
				return (T) type.cast(values[0].as(String.class, "text/plain"));
			return null;
		}
		if (type.isSetOrArrayOf(String.class)) {
			return (T) type.castArray(values);
		}
		Class<?> componentType = type.component().asClass();
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

	private <T> T read(Fluid value, Class<T> ctype, Type genericType,
			String... mediaTypes) throws TransformerConfigurationException,
			OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException,
			MimeTypeParseException, URISyntaxException {
		String media = getMediaType(ctype, genericType, mediaTypes);
		return (T) value.as(genericType, media);
	}

	private boolean isReadable(Class<?> componentType, String[] mediaTypes)
			throws MimeTypeParseException {
		String media = getMediaType(componentType, componentType, mediaTypes);
		return sample.toMedia(componentType, media) != null;
	}

	private String getMediaType(Class<?> type, Type genericType,
			String[] mediaTypes) throws MimeTypeParseException {
		Accepter accepter = new Accepter(mediaTypes);
		for (MimeType m : accepter.getAcceptable(this.mediaTypes)) {
			if (sample.toMedia(genericType, m.toString()) != null)
				return m.toString();
		}
		return null;
	}

}
