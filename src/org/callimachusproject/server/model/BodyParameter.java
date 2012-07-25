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
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.exceptions.UnsupportedMediaType;
import org.callimachusproject.server.util.Accepter;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Wraps an HttpEntity is a parameter.
 */
public abstract class BodyParameter implements Parameter {
	private final FluidFactory ff = FluidFactory.getInstance();
	private final String mimeType;
	private final boolean stream;
	private final String base;
	private final String location;
	private final ObjectConnection con;

	public BodyParameter(String mimeType, boolean stream,
			String base, String location, ObjectConnection con) {
		this.mimeType = mimeType;
		this.stream = stream;
		this.base = base;
		this.location = location;
		this.con = con;
	}

	public void close() throws IOException {
		// allow subclasses to override
	}

	public String getContentType() {
		return mimeType;
	}

	public Collection<MimeType> getReadableTypes(Class<?> ctype, Type gtype,
			Accepter accepter) throws MimeTypeParseException {
		Fluid reader = ff.builder(con).nil(mimeType);
		List<MimeType> acceptable = new ArrayList<MimeType>();
		for (MimeType media : accepter.getAcceptable(mimeType)) {
			if (!stream && location == null && mimeType == null) {
				acceptable.add(media);
				continue; // reads null
			}
			if (stream && ReadableByteChannel.class.equals(ctype)) {
				acceptable.add(media);
				continue;
			}
			if (reader.isProducible(gtype, media.toString())) {
				acceptable.add(media);
				continue;
			}
			FluidType mtype = new FluidType(gtype, media.toString());
			if (mtype.isSetOrArray()) {
				if (reader.isProducible(mtype.component())) {
					acceptable.add(media);
					continue;
				}
			}
		}
		return acceptable;
	}

	public <T> T read(Class<T> ctype, Type gtype, String[] mediaTypes)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, MimeTypeParseException,
			URISyntaxException {
		FluidType type = new FluidType(gtype, mimeType);
		if (location == null && !stream && mimeType == null)
			return null;
		ReadableByteChannel in = getReadableByteChannel();
		Fluid reader;
		if (location == null) {
			reader = ff.builder(con).channel(mimeType, in, base);
		} else {
			if (in != null) {
				in.close();
			}
			reader = ff.builder(con).uri(location, base);
		}
		if (stream && type.isOrIsSetOf(ReadableByteChannel.class))
			return (T) type.castComponent(in);
		for (MimeType media : new Accepter(mediaTypes).getAcceptable(mimeType)) {
			if (!reader.isProducible(gtype, media.toString()))
				continue;
			return (T) (reader.produce(gtype, media.toString()));
		}
		if (reader.isProducible(type))
			return (T) (reader.produce(type));
		if (type.isSetOrArray()) {
			Type cgtype = type.component().asType();
			Class<?> cctype = type.component().asClass();
			for (MimeType media : new Accepter(mediaTypes)
					.getAcceptable(mimeType)) {
				FluidType mctype = type.as(media.toString());
				if (!reader.isProducible(mctype))
					continue;
				return (T) type.castComponent(reader.produce(mctype));
			}
			FluidType mmtype = type.component();
			if (reader.isProducible(mmtype))
				return (T) type.castComponent(reader.produce(mmtype));
		}
		throw new UnsupportedMediaType();
	}

	protected abstract ReadableByteChannel getReadableByteChannel()
			throws IOException;

}
