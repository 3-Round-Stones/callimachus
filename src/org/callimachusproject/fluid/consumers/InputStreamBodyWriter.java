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

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
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
import org.callimachusproject.xml.DocumentFactory;
import org.callimachusproject.xml.XMLEventReaderFactory;
import org.openrdf.OpenRDFException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Writes an InputStream into an OutputStream.
 */
public class InputStreamBodyWriter implements Consumer<InputStream> {
	private final DocumentFactory docFactory = DocumentFactory.newInstance();
	private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
	private final XMLEventReaderFactory inFactory = XMLEventReaderFactory
			.newInstance();

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		return InputStream.class.isAssignableFrom(mtype.asClass());
	}

	public Fluid consume(final InputStream result, final String base, final FluidType ftype,
			final FluidBuilder builder) {
		return new AbstractFluid() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() throws IOException {
				if (result != null) {
					result.close();
				}
			}

			public String toChannelMedia(String... media) {
				return ftype.as(media).preferred();
			}

			public ReadableByteChannel asChannel(String... media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				return ChannelUtil.newChannel(result);
			}
	
			public String toStreamMedia(String... media) {
				return ftype.as(media).preferred();
			}

			public InputStream asStream(String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				return result;
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
				FluidType ctype = ftype.as(media);
				if (!ctype.isXML()) {
					Charset charset = ctype.getCharset();
					if (charset == null) {
						charset = Charset.defaultCharset();
					}
					return new InputStreamReader(result, charset);
				} else {
					CharArrayWriter caw = new CharArrayWriter(8192);
					try {
						writeTo(caw, media);
					} finally {
						caw.close();
					}
					return new CharArrayReader(caw.toCharArray());
				}
			}

			@Override
			public void writeTo(Writer writer, String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				if (!ftype.as(media).isXML()) {
					Reader reader = asReader(media);
					if (reader == null)
						return;
					try {
						int read;
						char[] cbuf = new char[1024];
						while ((read = reader.read(cbuf)) >= 0) {
							writer.write(cbuf, 0, read);
						}
					} finally {
						reader.close();
					}
				} else {
					XMLEventReader reader = asXMLEventReader();
					if (reader == null)
						return;
					try {
						XMLEventWriter xml = outFactory.createXMLEventWriter(writer);
						xml.add(reader);
						xml.flush();
					} finally {
						reader.close();
					}
				}
			}

			@Override
			public String toXMLEventReaderMedia(String... media) {
				return ftype.asXML().as(media).preferred();
			}

			@Override
			public XMLEventReader asXMLEventReader(String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				InputStream in = asStream();
				if (in == null)
					return null;
				if (base == null)
					return inFactory.createXMLEventReader(in);
				return inFactory.createXMLEventReader(base, in);
			}

			@Override
			public String toDocumentMedia(String... media) {
				return ftype.asXML().as(media).preferred();
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
						return docFactory.parse(in);
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
}
