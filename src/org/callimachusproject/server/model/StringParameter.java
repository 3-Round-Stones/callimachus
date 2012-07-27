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
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Provides an {@link Parameter} interface for a query parameter or header
 * value.
 */
public class StringParameter implements Parameter {
	private final FluidFactory ff = FluidFactory.getInstance();
	private final Fluid sample;
	private final Fluid[] values;

	public StringParameter(String[] values, String base, ObjectConnection con,
			String... mimeType) {
		FluidBuilder fb = ff.builder(con);
		if (values == null || values.length == 0) {
			this.values = new Fluid[0];
			this.sample = fb.media(mimeType);
		} else {
			this.values = new Fluid[values.length];
			for (int i = 0; i < values.length; i++) {
				this.values[i] = fb.consume(values[i], base, String.class,
						mimeType);
			}
			this.sample = this.values[0];
		}
	}

	public String getMediaType(FluidType ftype) {
		return sample.toMedia(ftype);
	}

	public Object read(FluidType type)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, URISyntaxException {
		if (type.is(String.class)) {
			if (values != null && values.length > 0)
				return type.cast(values[0].asString());
			return null;
		}
		if (type.isSetOrArrayOf(String.class)) {
			return type.castArray(values);
		}
		if (type.isArray() && isReadable(type.component()))
			return type.castArray(readArray(type.component()));
		if (type.isSet() && isReadable(type.component()))
			return type.castSet(readSet(type.component()));
		if (values != null && values.length > 0)
			return read(values[0], type);
		return null;
	}

	private Object readArray(FluidType ftype)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, URISyntaxException {
		if (values == null)
			return null;
		Object result = Array.newInstance(ftype.asClass(), values.length);
		for (int i = 0; i < values.length; i++) {
			Array.set(result, i, read(values[i], ftype));
		}
		return result;
	}

	private Set<Object> readSet(FluidType ftype)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, URISyntaxException {
		Set<Object> result = new LinkedHashSet<Object>(values.length);
		for (int i = 0; i < values.length; i++) {
			result.add(read(values[i], ftype));
		}
		return result;
	}

	private Object read(Fluid value, FluidType ftype)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException {
		return value.as(ftype);
	}

	private boolean isReadable(FluidType ftype) {
		return sample.toMedia(ftype) != null;
	}

}
