/*
   Copyright (c) 2012 3 Round Stones Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.fluid;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

/**
 * Null implementation for {@link Fluid}s.
 * 
 * @author James Leigh
 * 
 */
public abstract class AbstractFluid implements Fluid {

	public String toString() {
		String systemId = getSystemId();
		if (systemId == null)
			return String.valueOf(getFluidType());
		return systemId + " " + String.valueOf(getFluidType());
	}

	/**
	 * {@link ReadableByteChannel}
	 */
	public String toChannelMedia(String... media) {
		return toProducedMedia(new FluidType(ReadableByteChannel.class, media));
	}

	public ReadableByteChannel asChannel(String... media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		return (ReadableByteChannel) produce(new FluidType(
				ReadableByteChannel.class, media));
	}

	/**
	 * {@link InputStream}
	 */
	public String toStreamMedia(String... media) {
		return toProducedMedia(new FluidType(InputStream.class, media));
	}

	public InputStream asStream(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		return (InputStream) produce(new FluidType(InputStream.class, media));
	}

	/**
	 * {@link String}
	 */
	public String toStringMedia(String... media) {
		return toProducedMedia(new FluidType(String.class, media));
	}

	public String asString(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		return (String) produce(new FluidType(String.class, media));
	}

	/**
	 * {@link HttpEntity}
	 */
	public String toHttpEntityMedia(String... media) {
		return toProducedMedia(new FluidType(HttpEntity.class, media));
	}

	public HttpEntity asHttpEntity(String... media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException, SAXException {
		return (HttpEntity) produce(new FluidType(HttpEntity.class, media));
	}

	/**
	 * {@link HttpResponse}
	 */
	public String toHttpResponseMedia(String... media) {
		return toProducedMedia(new FluidType(HttpResponse.class, media));
	}

	public HttpResponse asHttpResponse(String... media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException, SAXException {
		return (HttpResponse) produce(new FluidType(HttpResponse.class, media));
	}

	/**
	 * {@link Type}
	 */
	public final String toMedia(Type gtype, String... media) {
		return toMedia(new FluidType(gtype, media));
	}

	public final Object as(Type gtype, String... media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		return as(new FluidType(gtype, media));
	}

	/**
	 * {@link FluidType}
	 */
	public final String toMedia(FluidType ftype) {

		if (ftype.is(ReadableByteChannel.class))
			return toChannelMedia(ftype.media());

		if (ftype.is(InputStream.class))
			return toStreamMedia(ftype.media());

		if (ftype.is(String.class))
			return toStringMedia(ftype.media());

		if (ftype.is(HttpEntity.class))
			return toHttpEntityMedia(ftype.media());

		if (ftype.is(HttpResponse.class))
			return toHttpResponseMedia(ftype.media());

		return toProducedMedia(ftype);
	}

	public final Object as(FluidType ftype) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {

		if (ftype.is(ReadableByteChannel.class))
			return asChannel(ftype.media());

		if (ftype.is(InputStream.class))
			return asStream(ftype.media());

		if (ftype.is(String.class))
			return asString(ftype.media());

		if (ftype.is(HttpEntity.class))
			return asHttpEntity(ftype.media());

		if (ftype.is(HttpResponse.class))
			return asHttpResponse(ftype.media());

		return produce(ftype);
	}

	protected String toProducedMedia(FluidType ftype) {
		return null;
	}

	protected Object produce(FluidType ftype) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		asVoid();
		return null;
	}

}
