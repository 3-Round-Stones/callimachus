/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.callimachusproject.fluid.consumers;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.ProducerChannel;
import org.callimachusproject.server.util.ProducerChannel.WritableProducer;
import org.openrdf.OpenRDFException;

/**
 * Writes an XMLEventReader into an OutputStream.
 */
public class XMLEventMessageWriter implements Consumer<XMLEventReader> {
	private XMLOutputFactory factory;
	{
		factory = XMLOutputFactory.newInstance();
		// javax.xml.stream.isRepairingNamespaces can cause xmlns="" 
		// if first element uses default namespace and has attributes, this leads to NPE when parsed
	}

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		String mediaType = mtype.getMediaType();
		if (!XMLEventReader.class.isAssignableFrom((Class<?>) mtype.getClassType()))
			return false;
		if (mediaType != null && !mediaType.startsWith("*")
				&& !mediaType.startsWith("text/")
				&& !mediaType.startsWith("application/"))
			return false;
		return true;
	}

	private String getMediaType(FluidType mtype, FluidBuilder builder) {
		String mimeType = mtype.getMediaType();
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("application/*"))
			return "application/xml";
		if (mimeType.startsWith("text/")) {
			Charset charset = mtype.getCharset();
			if (charset == null) {
				charset = Charset.defaultCharset();
			}
			if (mimeType.startsWith("text/*"))
				return "text/xml;charset=" + charset.name();
			return mimeType + ";charset=" + charset.name();
		}
		return mimeType;
	}

	public Fluid consume(final FluidType ftype, final XMLEventReader result, final String base,
			final FluidBuilder builder) {
		return new AbstractFluid(builder) {
			public HttpEntity asHttpEntity(String media) throws IOException,
					OpenRDFException, XMLStreamException, TransformerException,
					ParserConfigurationException {
				String mediaType = getMediaType(ftype.as(media), builder);
				return new ReadableHttpEntityChannel(mediaType, -1, write(ftype.as(mediaType), result, base));
			}

			public String toString() {
				return result.toString();
			}
		};
	}

	private ReadableByteChannel write(final FluidType mtype,
			final XMLEventReader result, final String base) throws IOException {
		if (result == null)
			return null;
		return new ProducerChannel(new WritableProducer() {
			public void produce(WritableByteChannel out) throws IOException {
				try {
					writeTo(mtype, result, base, out, 1024);
				} catch (XMLStreamException e) {
					throw new IOException(e);
				} finally {
					out.close();
				}
			}

			public String toString() {
				return result.toString();
			}
		});
	}

	private void writeTo(FluidType mtype, XMLEventReader result, String base,
			WritableByteChannel out, int bufSize)
			throws IOException, XMLStreamException {
		try {
			Charset charset = mtype.getCharset();
			if (charset == null) {
				charset = Charset.defaultCharset();
			}
			XMLEventWriter writer = factory.createXMLEventWriter(ChannelUtil
					.newOutputStream(out), charset.name());
			try {
				writer.add(result);
				writer.flush();
			} finally {
				writer.close();
			}
		} finally {
			result.close();
		}
	}
}
