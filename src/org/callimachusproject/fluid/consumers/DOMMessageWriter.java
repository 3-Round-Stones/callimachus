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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.ProducerChannel;
import org.callimachusproject.server.util.ProducerChannel.WritableProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Prints DOM Node into an OutputStream.
 */
public class DOMMessageWriter implements Consumer<Node> {

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
		if (!Document.class.isAssignableFrom(type)
				&& !Element.class.isAssignableFrom(type))
			return false;
		return mtype.is("application/*", "text/*");
	}

	public Fluid consume(final Node result, final String base, final FluidType ftype,
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
				return getMediaType(ftype.as(media));
			}

			public ReadableByteChannel asChannel(String... media)
					throws IOException {
				return write(ftype.as(toChannelMedia(media)), result, base);
			}
	
			public String toString() {
				return String.valueOf(result);
			}
		};
	}

	String getMediaType(FluidType ftype) {
		FluidType xml = ftype.as("application/xml", "application/*", "text/xml", "text/*");
		String mimeType = xml.preferred();
		if (mimeType.startsWith("text/") && !mimeType.contains("charset=")) {
			Charset charset = xml.getCharset();
			if (charset == null) {
				charset = Charset.defaultCharset();
			}
			return mimeType + ";charset=" + charset.name();
		}
		return mimeType;
	}

	ReadableByteChannel write(final FluidType mtype,
			final Node result, final String base)
			throws IOException {
		return new ProducerChannel(new WritableProducer() {
			public void produce(WritableByteChannel out) throws IOException {
				try {
					writeTo(mtype, result, base, out, 1024);
				} catch (TransformerException e) {
					throw new IOException(e);
				} catch (ParserConfigurationException e) {
					throw new IOException(e);
				} finally {
					out.close();
				}
			}

			public String toString() {
				if (result == null)
					return super.toString();
				return result.toString();
			}
		});
	}

	private void writeTo(FluidType mtype, Node node, String base,
			WritableByteChannel out, int bufSize)
			throws IOException, TransformerException,
			ParserConfigurationException {
		Charset charset = mtype.getCharset();
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		Source source = new DOMSource(node, base);
		Result result = new StreamResult(ChannelUtil.newOutputStream(out));
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(ENCODING, charset.name());
		ErrorCatcher listener = new ErrorCatcher();
		transformer.setErrorListener(listener);
		transformer.transform(source, result);
		if (listener.isFatal())
			throw listener.getFatalError();
	}
}
