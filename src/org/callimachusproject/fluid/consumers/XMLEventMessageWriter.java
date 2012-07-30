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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.ProducerChannel;
import org.callimachusproject.server.util.ProducerChannel.WritableProducer;
import org.callimachusproject.xslt.DocumentFactory;
import org.openrdf.OpenRDFException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Writes an XMLEventReader into an OutputStream.
 */
public class XMLEventMessageWriter implements Consumer<XMLEventReader> {
	private final DocumentFactory docFactory = DocumentFactory.newInstance();
	private XMLOutputFactory factory;
	{
		factory = XMLOutputFactory.newInstance();
		// javax.xml.stream.isRepairingNamespaces can cause xmlns="" 
		// if first element uses default namespace and has attributes, this leads to NPE when parsed
	}

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		if (!XMLEventReader.class.isAssignableFrom(mtype.asClass()))
			return false;
		return mtype.is("application/*", "text/*", "image/xml");
	}

	public Fluid consume(final XMLEventReader result, final String base, final FluidType ftype,
			final FluidBuilder builder) {
		return new AbstractFluid() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() {
				// no-op
			}

			public String toChannelMedia(String... media) {
				FluidType ctype = ftype.as(media);
				String mimeType = ctype.preferred();
				if (mimeType != null && mimeType.startsWith("text/") && !mimeType.contains("charset=")) {
					Charset charset = ctype.getCharset();
					if (charset == null) {
						charset = Charset.defaultCharset();
					}
					return mimeType + ";charset=" + charset.name();
				}
				return mimeType;
			}

			public ReadableByteChannel asChannel(String... media)
					throws IOException {
				return write(ftype.as(toChannelMedia(media)), result, base);
			}
	
			@Override
			public String toStreamMedia(String... media) {
				return toChannelMedia(media);
			}

			@Override
			public InputStream asStream(String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				if (result == null)
					return null;
				ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
				try {
					asStream(baos, media);
				} finally {
					baos.close();
				}
				return new ByteArrayInputStream(baos.toByteArray());
			}

			@Override
			public void asStream(OutputStream out, String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				FluidType ctype = ftype.as(media);
				Charset charset = ctype.getCharset();
				if (charset == null) {
					charset = Charset.defaultCharset();
				}
				XMLEventReader reader = asXMLEventReader(media);
				if (reader == null)
					return;
				try {
					XMLEventWriter writer = factory.createXMLEventWriter(out,
							charset.name());
					writer.add(reader);
					writer.flush();
				} finally {
					reader.close();
				}
			}

			@Override
			public String toReaderMedia(String... media) {
				return ftype.as(media).preferred();
			}

			@Override
			public Reader asReader(String... media) throws OpenRDFException,
					IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				if (result == null)
					return null;
				CharArrayWriter caw = new CharArrayWriter(8192);
				try {
					asWriter(caw, media);
				} finally {
					caw.close();
				}
				return new CharArrayReader(caw.toCharArray());
			}

			@Override
			public void asWriter(Writer writer, String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				XMLEventReader reader = asXMLEventReader();
				if (reader == null)
					return;
				try {
					XMLEventWriter xml = factory.createXMLEventWriter(writer);
					xml.add(reader);
					xml.flush();
				} finally {
					reader.close();
				}
			}

			@Override
			public String toXMLEventReaderMedia(String... media) {
				return ftype.as(media).preferred();
			}

			@Override
			public XMLEventReader asXMLEventReader(String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				return result;
			}

			@Override
			public String toDocumentMedia(String... media) {
				return ftype.as(media).preferred();
			}

			@Override
			public Document asDocument(String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				InputStream in = asStream();
				if (in == null)
					return null;
				try {
					if (base == null)
						docFactory.parse(in);
					return docFactory.parse(in, base);
				} finally {
					in.close();
				}
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}

	ReadableByteChannel write(final FluidType mtype,
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
