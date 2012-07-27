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

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Wraps a message response to output to an HTTP response.
 */
public class ResponseParameter implements Parameter {
	private interface SetString extends Set<String> {
	}

	private static Type setOfStringType = SetString.class
			.getGenericInterfaces()[0];
	private final FluidFactory ff = FluidFactory.getInstance();
	private final Fluid writer;
	private final Object result;
	private final Class<?> type;
	private final Map<String, String> headers = new HashMap<String, String>();
	private final List<String> expects = new ArrayList<String>();

	public ResponseParameter(String[] mimeTypes, Object result, Class<?> type,
			Type genericType, String base, ObjectConnection con) {
		this.result = result;
		this.type = type;
		if (mimeTypes == null || mimeTypes.length < 1) {
			mimeTypes = new String[] { "*/*" };
		}
		FluidBuilder builder = ff.builder(con);
		if (!builder.isConsumable(genericType, mimeTypes))
			throw new ClassCastException(type.getSimpleName()
					+ " cannot be converted into " + mimeTypes);
		this.writer = builder.consume(result, base, genericType, mimeTypes);
	}

	public String toString() {
		return String.valueOf(writer);
	}

	public HttpEntity asHttpEntity(String media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException, SAXException {
		return writer.asHttpEntity(media);
	}

	public String getMediaType(FluidType ftype) {
		return writer.toMedia(ftype);
	}

	public Object read(FluidType ftype)
			throws OpenRDFException, TransformerConfigurationException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, URISyntaxException {
		if (writer.toMedia(ftype) == null)
			throw new ClassCastException(String.valueOf(result)
					+ " cannot be converted into " + type.getSimpleName());
		return writer.as(ftype);
	}

	public boolean isNoContent() {
		return result == null || Set.class.equals(type)
				&& ((Set<?>) result).isEmpty();
	}

	public Set<String> getLocations() throws TransformerConfigurationException,
			OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException {
		FluidType ftype = new FluidType(setOfStringType, "text/uri-list");
		if (writer.toMedia(ftype) == null)
			return null;
		return (Set<String>) writer.as(ftype);
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
