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
 * When a {@link Fluid} does not support a desired Java or media type, it is
 * converted into a {@link ReadableByteChannel} and parsed to the desired Java
 * type.
 * 
 * @author James Leigh
 * 
 */
class ChannelFluid implements Fluid {
	private final Fluid fluid;
	private final FluidBuilder builder;

	public ChannelFluid(Fluid fluid, FluidBuilder builder) {
		assert fluid != null;
		assert builder != null;
		this.fluid = fluid;
		this.builder = builder;
	}

	public FluidType getFluidType() {
		return fluid.getFluidType();
	}

	public String getSystemId() {
		return fluid.getSystemId();
	}

	public String toString() {
		return fluid.toString();
	}

	public void asVoid() throws TransformerConfigurationException,
			OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException {
		fluid.asVoid();
	}

	/**
	 * {@link ReadableByteChannel}
	 */
	public String toChannelMedia(String... media) {
		String ret = fluid.toChannelMedia(media);
		if (ret != null)
			return ret;
		return toMediaFluid(media).toChannelMedia(media);
	}

	public ReadableByteChannel asChannel(String... media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		if (fluid.toChannelMedia(media) != null)
			return fluid.asChannel(media);
		return asChannelFluid(media).asChannel(media);
	}

	/**
	 * {@link InputStream}
	 */
	public String toStreamMedia(String... media) {
		String ret = fluid.toStreamMedia(media);
		if (ret != null)
			return ret;
		return toMediaFluid(media).toStreamMedia(media);
	}

	public InputStream asStream(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		if (fluid.toStreamMedia(media) != null)
			return fluid.asStream(media);
		return asChannelFluid(media).asStream(media);
	}

	/**
	 * {@link String}
	 */
	public String toStringMedia(String... media) {
		String ret = fluid.toStringMedia(media);
		if (ret != null)
			return ret;
		return toMediaFluid(media).toStringMedia(media);
	}

	public String asString(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		if (fluid.toStringMedia(media) != null)
			return fluid.asString(media);
		return asChannelFluid(media).asString(media);
	}

	/**
	 * {@link HttpEntity}
	 */
	public String toHttpEntityMedia(String... media) {
		String ret = fluid.toHttpEntityMedia(media);
		if (ret != null)
			return ret;
		return toMediaFluid(media).toHttpEntityMedia(media);
	}

	public HttpEntity asHttpEntity(String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		if (fluid.toHttpEntityMedia(media) != null)
			return fluid.asHttpEntity(media);
		return asChannelFluid(media).asHttpEntity(media);
	}

	/**
	 * {@link HttpResponse}
	 */
	public String toHttpResponseMedia(String... media) {
		String ret = fluid.toHttpResponseMedia(media);
		if (ret != null)
			return ret;
		return toMediaFluid(media).toHttpResponseMedia(media);
	}

	public HttpResponse asHttpResponse(String... media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		if (fluid.toHttpResponseMedia(media) != null)
			return fluid.asHttpResponse(media);
		return asChannelFluid(media).asHttpResponse(media);
	}

	/**
	 * {@link Type}
	 */
	public String toMedia(Type gtype, String... media) {
		String ret = fluid.toMedia(gtype, media);
		if (ret != null)
			return ret;
		return toMediaFluid(media).toMedia(gtype, media);
	}

	public Object as(Type gtype, String... media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		if (fluid.toMedia(gtype, media) != null)
			return fluid.as(gtype, media);
		return asChannelFluid(media).as(gtype, media);
	}

	/**
	 * {@link FluidType}
	 */
	public String toMedia(FluidType ftype) {
		String ret = fluid.toMedia(ftype);
		if (ret != null)
			return ret;
		return toMediaFluid(ftype.media()).toMedia(ftype);
	}

	public Object as(FluidType ftype) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		if (fluid.toMedia(ftype) != null)
			return fluid.as(ftype);
		return asChannelFluid(ftype.media()).as(ftype);
	}

	/**
	 * {@link Fluid}
	 */
	private Fluid toMediaFluid(String[] media) {
		return builder.media(getChannelMedia(media));
	}

	private Fluid asChannelFluid(String[] media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		String[] cmt = getChannelMedia(media);
		return builder.channel(fluid.asChannel(cmt), fluid.getSystemId(), cmt);
	}

	private String[] getChannelMedia(String[] media) {
		if (getFluidType().is(media))
			return getFluidType().as(media).media();
		return getFluidType().media();
	}

}
