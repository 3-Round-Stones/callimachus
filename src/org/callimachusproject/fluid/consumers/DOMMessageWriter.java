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

import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.ProducerChannel;
import org.callimachusproject.server.util.ProducerChannel.WritableProducer;
import org.openrdf.repository.object.ObjectConnection;
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

	public boolean isText(FluidType mtype) {
		return true;
	}

	public long getSize(FluidType mtype, ObjectConnection con, Node result, Charset charset) {
		return -1;
	}

	public boolean isWriteable(FluidType mtype, ObjectConnection con) {
		String mediaType = mtype.getMediaType();
		Class<?> type = mtype.getClassType();
		if (!Document.class.isAssignableFrom(type)
				&& !Element.class.isAssignableFrom(type))
			return false;
		if (mediaType != null && !mediaType.startsWith("*")
				&& !mediaType.startsWith("text/")
				&& !mediaType.startsWith("application/"))
			return false;
		return true;
	}

	public String getContentType(FluidType mtype, Charset charset) {
		String mimeType = mtype.getMediaType();
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("application/*"))
			return "application/xml";
		if (mimeType.startsWith("text/")) {
			if (charset == null) {
				charset = Charset.defaultCharset();
			}
			if (mimeType.startsWith("text/*"))
				return "text/xml;charset=" + charset.name();
			return mimeType + ";charset=" + charset.name();
		}
		return mimeType;
	}

	public ReadableByteChannel write(final FluidType mtype,
			ObjectConnection con, final Node result, final String base, final Charset charset)
			throws IOException {
		return new ProducerChannel(new WritableProducer() {
			public void produce(WritableByteChannel out) throws IOException {
				try {
					writeTo(mtype, result, base, charset, out, 1024);
				} catch (TransformerException e) {
					throw new IOException(e);
				} catch (ParserConfigurationException e) {
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

	public void writeTo(FluidType mtype, Node node, String base,
			Charset charset, WritableByteChannel out, int bufSize)
			throws IOException, TransformerException,
			ParserConfigurationException {
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
