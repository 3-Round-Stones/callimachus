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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
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
import org.callimachusproject.server.util.ProducerStream;
import org.callimachusproject.server.util.ProducerStream.OutputProducer;
import org.callimachusproject.xslt.DocumentFactory;
import org.callimachusproject.xslt.XMLEventReaderFactory;
import org.openrdf.OpenRDFException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Writes a Readable object into an OutputStream.
 */
public class ReadableBodyWriter implements Consumer<Readable> {
	private final DocumentFactory docFactory = DocumentFactory.newInstance();
	private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
	private final XMLEventReaderFactory inFactory = XMLEventReaderFactory
			.newInstance();

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		if (!Readable.class.isAssignableFrom(mtype.asClass()))
			return false;
		return mtype.is("text/*");
	}

	public Fluid consume(final Readable result, final String base,
			FluidType type, final FluidBuilder builder) {
		final FluidType ftype = type.as("text/plain", "text/*");
		return new AbstractFluid() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() throws IOException {
				if (result instanceof Closeable) {
					((Closeable) result).close();
				}
			}

			public String toChannelMedia(String... media) {
				return toStreamMedia(media);
			}

			public ReadableByteChannel asChannel(final String... media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				return new ProducerChannel(new WritableProducer() {
					public void produce(WritableByteChannel out)
							throws IOException {
						try {
							asStream(ChannelUtil.newOutputStream(out), media);
						} catch (XMLStreamException e) {
							throw new IOException(e);
						}
					}

					public String toString() {
						return String.valueOf(result);
					}
				});
			}

			public String toStreamMedia(String... media) {
				FluidType ctype = ftype.as(media);
				String mimeType = ctype.preferred();
				Charset charset = ctype.getCharset();
				if (charset == null) {
					charset = Charset.defaultCharset();
				}
				if (mimeType == null || mimeType.contains("charset="))
					return mimeType;
				return mimeType + ";charset=" + charset.name();

			}

			public InputStream asStream(final String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				return new ProducerStream(new OutputProducer() {
					public void produce(OutputStream out) throws IOException {
						try {
							asStream(out, media);
						} catch (XMLStreamException e) {
							throw new IOException(e);
						}
					}

					public String toString() {
						return String.valueOf(result);
					}
				});
			}

			public void asStream(OutputStream out, String... media)
					throws IOException, XMLStreamException {
				if (result == null)
					return;
				try {
					FluidType ctype = ftype.as(media);
					Charset charset = ctype.getCharset();
					if (charset == null) {
						charset = Charset.defaultCharset();
					}
					if (!ctype.isXML()) {
						Writer writer = new OutputStreamWriter(out, charset);
						CharBuffer cb = CharBuffer.allocate(1024);
						while (result.read(cb) >= 0) {
							cb.flip();
							writer.write(cb.array(), cb.position(), cb.limit());
							cb.clear();
						}
						writer.flush();
					} else {
						XMLEventReader reader = asXMLEventReader();
						if (reader == null)
							return;
						try {
							XMLEventWriter writer = outFactory
									.createXMLEventWriter(out, charset.name());
							writer.add(reader);
							writer.flush();
						} finally {
							reader.close();
						}
					}
				} finally {
					asVoid();
				}
			}

			public String toReaderMedia(String... media) {
				return ftype.as("text/plain", "text/*").as(media).preferred();
			}

			@Override
			public Reader asReader(String... media) {
				if (result instanceof Reader)
					return (Reader) result;
				return new Reader() {
					public void close() throws IOException {
						asVoid();
					}

					public int read(char[] cbuf, int off, int len)
							throws IOException {
						return result.read(CharBuffer.wrap(cbuf, off, len));
					}

					public int read(CharBuffer cbuf) throws IOException {
						return result.read(cbuf);
					}

					public String toString() {
						return String.valueOf(result);
					}
				};
			}

			public String toXMLEventReaderMedia(String... media) {
				return ftype.asXML().as(media).preferred();
			}

			public XMLEventReader asXMLEventReader(String... media)
					throws XMLStreamException {
				Reader source = asReader(media);
				if (source == null)
					return null;
				if (base == null)
					return inFactory.createXMLEventReader(source);
				return inFactory.createXMLEventReader(base, source);
			}

			public String toDocumentMedia(String... media) {
				return toXMLEventReaderMedia(media);
			}

			public Document asDocument(String... media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				Reader reader = asReader();
				if (reader == null)
					return null;
				try {
					if (base == null)
						return docFactory.parse(reader);
					return docFactory.parse(reader, base);
				} finally {
					asVoid();
				}
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}
}
