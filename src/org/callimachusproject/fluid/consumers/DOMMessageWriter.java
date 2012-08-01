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

import static javax.xml.transform.OutputKeys.ENCODING;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.callimachusproject.fluid.Vapor;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.ProducerChannel;
import org.callimachusproject.server.util.ProducerChannel.WritableProducer;
import org.callimachusproject.server.util.ProducerStream;
import org.callimachusproject.server.util.ProducerStream.OutputProducer;
import org.callimachusproject.xml.DocumentFactory;
import org.openrdf.OpenRDFException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Prints DOM Node into an OutputStream.
 */
public class DOMMessageWriter implements Consumer<Node> {
	private static final DocumentFactory docFactory = DocumentFactory
			.newInstance();

	private static class ErrorCatcher implements ErrorListener {
		private Logger logger = LoggerFactory.getLogger(ErrorCatcher.class);
		private TransformerException fatal;

		public boolean isFatal() {
			return fatal != null;
		}

		public TransformerException getFatalError() {
			return fatal;
		}

		public void error(TransformerException exception) {
			logger.warn(exception.toString(), exception);
		}

		public void fatalError(TransformerException exception) {
			if (this.fatal == null) {
				this.fatal = exception;
			}
			logger.error(exception.toString(), exception);
		}

		public void warning(TransformerException exception) {
			logger.info(exception.toString(), exception);
		}
	}

	private TransformerFactory factory = TransformerFactory.newInstance();

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		Class<?> type = mtype.asClass();
		return mtype.isXML() && Node.class.isAssignableFrom(type);
	}

	public Fluid consume(final Node node, final String base,
			final FluidType ftype, final FluidBuilder builder) {
		return new Vapor() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() {
				// no-op
			}

			@Override
			protected String toChannelMedia(FluidType media) {
				FluidType xml = ftype.as(media);
				String mimeType = xml.preferred();
				if (mimeType != null && mimeType.startsWith("text/")
						&& !mimeType.contains("charset=")) {
					Charset charset = xml.getCharset();
					if (charset == null) {
						charset = Charset.defaultCharset();
					}
					return mimeType + ";charset=" + charset.name();
				}
				return mimeType;
			}

			@Override
			protected ReadableByteChannel asChannel(final FluidType media)
					throws IOException {
				if (node == null)
					return null;
				return new ProducerChannel(new WritableProducer() {
					public void produce(WritableByteChannel out)
							throws IOException {
						try {
							streamTo(ChannelUtil.newOutputStream(out), media);
						} catch (TransformerException e) {
							throw new IOException(e);
						} finally {
							out.close();
						}
					}

					public String toString() {
						return String.valueOf(node);
					}
				});
			}

			@Override
			protected void transferTo(WritableByteChannel out, FluidType media)
					throws IOException, TransformerException {
				streamTo(ChannelUtil.newOutputStream(out), media);
			}

			@Override
			protected String toStreamMedia(FluidType media) {
				return toChannelMedia(media);
			}

			@Override
			protected InputStream asStream(final FluidType media)
					throws IOException {
				if (node == null)
					return null;
				return new ProducerStream(new OutputProducer() {
					public void produce(OutputStream out) throws IOException {
						try {
							streamTo(out, media);
						} catch (TransformerException e) {
							throw new IOException(e);
						} finally {
							out.close();
						}
					}

					public String toString() {
						return String.valueOf(node);
					}
				});
			}

			@Override
			protected void streamTo(OutputStream out, FluidType media)
					throws TransformerException {
				if (node == null)
					return;
				Charset charset = ftype.as(toChannelMedia(media)).getCharset();
				if (charset == null) {
					charset = Charset.defaultCharset();
				}
				Source source = new DOMSource(node, base);
				Result result = new StreamResult(out);
				Transformer transformer = factory.newTransformer();
				transformer.setOutputProperty(ENCODING, charset.name());
				ErrorCatcher listener = new ErrorCatcher();
				transformer.setErrorListener(listener);
				transformer.transform(source, result);
				if (listener.isFatal())
					throw listener.getFatalError();
			}

			@Override
			protected String toDocumentMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected Document asDocument(FluidType media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				if (node == null)
					return null;
				if (node instanceof Document) {
					return (Document) node;
				} else if (node instanceof DocumentFragment) {
					NodeList nodes = node.getChildNodes();
					Document doc = docFactory.newDocument();
					if (nodes.getLength() == 1
							&& node.getFirstChild().getNodeType() == 1) {
						doc.appendChild(doc.importNode(node.getFirstChild(),
								true));
					} else {
						Element root = doc.createElement("root");
						root.appendChild(doc.importNode(node, true));
						doc.appendChild(root);
					}
					return doc;
				} else {
					Document doc = docFactory.newDocument();
					doc.appendChild(doc.importNode(node, true));
					return doc;
				}
			}

			@Override
			protected String toDocumentFragmentMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected DocumentFragment asDocumentFragment(FluidType media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				if (node == null)
					return null;
				if (node instanceof DocumentFragment)
					return (DocumentFragment) node;
				if (node instanceof Document) {
					Document doc = (Document) node;
					DocumentFragment frag = doc.createDocumentFragment();
					frag.appendChild(doc.getDocumentElement());
					return frag;
				} else {
					Document doc = node.getOwnerDocument();
					DocumentFragment frag = doc.createDocumentFragment();
					frag.appendChild(node);
					return frag;
				}
			}

			@Override
			protected String toElementMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected Element asElement(FluidType media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				if (node instanceof Element)
					return (Element) node;
				Document doc = asDocument(media);
				if (doc == null)
					return null;
				return doc.getDocumentElement();
			}

			public String toString() {
				return String.valueOf(node);
			}
		};
	}
}
