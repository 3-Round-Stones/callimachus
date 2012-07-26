/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.GenericType;
import org.callimachusproject.server.util.Accepter;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;

/**
 * Parameter with no value
 */
public class NullParameter implements Parameter {
	private final FluidFactory ff = FluidFactory.getInstance();
	private final FluidBuilder fb;

	public NullParameter(ObjectConnection con) {
		fb = ff.builder(con);
	}

	public Collection<? extends MimeType> getReadableTypes(Class<?> ctype,
			Type gtype, Accepter accepter) throws MimeTypeParseException {
		GenericType type = new GenericType(gtype);
		List<MimeType> acceptable = new ArrayList<MimeType>();
		for (MimeType m : accepter.getAcceptable("*/*")) {
			if (fb.media("*/*").toMedia(gtype, m.toString()) != null) {
				acceptable.add(m);
			} else if (type.isSetOrArray()) {
				if (fb.media("*/*").toMedia(type.component().asType(), m.toString()) != null) {
					acceptable.add(m);
				}
			}
		}
		return acceptable;
	}

	public <T> T read(Class<T> ctype, Type genericType, String[] mediaTypes)
			throws MimeTypeParseException, IOException, OpenRDFException, XMLStreamException, TransformerException, ParserConfigurationException {
		GenericType type = new GenericType(genericType);
		Class<?> componentType = type.component().asClass();
		if (type.isArray() && isReadable(componentType, mediaTypes))
			return (T) type.castArray(readArray(componentType, mediaTypes));
		if (type.isSet() && isReadable(componentType, mediaTypes))
			return (T) type.castSet(readSet(componentType, mediaTypes));
		return null;
	}

	private <T> T[] readArray(Class<T> componentType, String[] mediaTypes) {
		return null;
	}

	private <T> Set<T> readSet(Class<T> componentType, String[] mediaTypes) {
		return Collections.emptySet();
	}

	private boolean isReadable(Class<?> componentType, String[] mediaTypes)
			throws MimeTypeParseException, IOException, OpenRDFException, XMLStreamException, TransformerException, ParserConfigurationException {
		String media = getMediaType(componentType, componentType, mediaTypes);
		return fb.media("*/*").toMedia(componentType, media) != null;
	}

	private String getMediaType(Class<?> type, Type genericType,
			String[] mediaTypes) throws MimeTypeParseException, IOException, OpenRDFException, XMLStreamException, TransformerException, ParserConfigurationException {
		Accepter accepter = new Accepter(mediaTypes);
		for (MimeType m : accepter.getAcceptable("*/*")) {
			String media = fb.media("*/*").toMedia(genericType, m.toString());
			if (media != null)
				return media;
		}
		return null;
	}

}
