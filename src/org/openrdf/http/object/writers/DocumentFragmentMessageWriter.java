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
package org.openrdf.http.object.writers;

import static javax.xml.transform.OutputKeys.ENCODING;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.MessageType;
import org.openrdf.http.object.util.ProducerChannel;
import org.openrdf.http.object.util.ProducerChannel.WritableProducer;
import org.openrdf.repository.object.xslt.DocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * Prints DocumentFragment into an OutputStream.
 */
public class DocumentFragmentMessageWriter implements
		MessageBodyWriter<DocumentFragment> {
	private static final String XSL_FRAGMENT = "<stylesheet version='1.0' xmlns='http://www.w3.org/1999/XSL/Transform'>"
			+ "<template match='/root'><copy-of select='*|text()|comment()'/></template></stylesheet>";

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
	private DocumentFactory builder;
	private Templates fragments;

	public DocumentFragmentMessageWriter()
			throws TransformerConfigurationException {
		Reader reader = new StringReader(XSL_FRAGMENT);
		fragments = factory.newTemplates(new StreamSource(reader));
		builder = DocumentFactory.newInstance();
	}

	public boolean isText(MessageType mtype) {
		return true;
	}

	public long getSize(MessageType mtype, DocumentFragment result,
			Charset charset) {
		return -1;
	}

	public boolean isWriteable(MessageType mtype) {
		String mediaType = mtype.getMimeType();
		if (DocumentFragment.class.equals(mtype.clas()))
			return true;
		if (mediaType == null)
			return false;
		if (!mediaType.startsWith("text/")
				&& !mediaType.startsWith("application/")
				&& !mediaType.startsWith("*"))
			return false;
		if (mtype.isUnknown() && (mediaType.contains("+xml")
				|| mediaType.contains("/xml")))
			return true;
		return DocumentFragment.class.isAssignableFrom((Class<?>) mtype.clas());
	}

	public String getContentType(MessageType mtype, Charset charset) {
		String mimeType = mtype.getMimeType();
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*"))
			return "text/xml-external-parsed-entity;charset=" + charset.name();
		if (mimeType.startsWith("text/"))
			return mimeType + ";charset=" + charset.name();
		if (mimeType.startsWith("application/*"))
			return "application/xml-external-parsed-entity";
		return mimeType;
	}

	public ReadableByteChannel write(final MessageType mtype,
			final DocumentFragment result, final String base,
			final Charset charset) throws IOException {
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

	public void writeTo(MessageType mtype, DocumentFragment node, String base,
			Charset charset, WritableByteChannel out, int bufSize)
			throws IOException, TransformerException,
			ParserConfigurationException {
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		Source source = createSource(node, base);
		Result result = new StreamResult(ChannelUtil.newOutputStream(out));
		Transformer transformer = createTransformer(node);
		transformer.setOutputProperty(ENCODING, charset.name());
		transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
		ErrorCatcher listener = new ErrorCatcher();
		transformer.setErrorListener(listener);
		transformer.transform(source, result);
		if (listener.isFatal())
			throw listener.getFatalError();
	}

	private DOMSource createSource(DocumentFragment node, String base)
			throws ParserConfigurationException {
		if (node.getChildNodes().getLength() == 1)
			return new DOMSource(node.getFirstChild(), base);
		Document doc = builder.newDocument();
		Element root = doc.createElement("root");
		root.appendChild(doc.importNode(node, true));
		return new DOMSource(root, base);
	}

	private Transformer createTransformer(DocumentFragment node)
			throws TransformerConfigurationException {
		if (node.getChildNodes().getLength() == 1)
			return factory.newTransformer();
		return fragments.newTransformer();
	}
}
