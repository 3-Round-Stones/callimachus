/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.callimachusproject.fluid.producers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Producer;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.xml.DocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.SAXException;

/**
 * Parses a DocumentFragment from an InputStream.
 */
public class DocumentFragmentMessageReader implements Producer {

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
	private DocumentFactory builder = DocumentFactory.newInstance();

	public boolean isProducable(FluidType ftype, FluidBuilder builder) {
		Class<?> type = ftype.asClass();
		if (!ftype.isXML())
			return false;
		return type.isAssignableFrom(DocumentFragment.class);
	}

	public DocumentFragment produce(FluidType ftype, ReadableByteChannel ch,
			Charset charset, String base, FluidBuilder builder)
			throws TransformerException, SAXException, IOException,
			ParserConfigurationException {
		try {
			InputStream in = ChannelUtil.newInputStream(ch);
			Document doc = createDocument(in, charset, base);
			if (doc == null)
				return null;
			DocumentFragment node = createNode();
			DOMResult result = new DOMResult(node);
			Source source = createSource(base, doc);
			Transformer transformer = factory.newTransformer();
			ErrorCatcher listener = new ErrorCatcher();
			transformer.setErrorListener(listener);
			transformer.transform(source, result);
			if (listener.isFatal())
				throw listener.getFatalError();
			return node;
		} finally {
			if (ch != null) {
				ch.close();
			}
		}
	}

	private DOMSource createSource(String base, Document doc) {
		if (base == null)
			return new DOMSource(doc);
		return new DOMSource(doc, base);
	}

	private DocumentFragment createNode() throws ParserConfigurationException {
		Document doc = builder.newDocument();
		return doc.createDocumentFragment();
	}

	private Document createDocument(InputStream in, Charset charset,
			String base) throws TransformerException, SAXException,
			IOException, ParserConfigurationException {
		if (charset == null) {
			try {
				if (base == null)
					return builder.parse(in);
				return builder.parse(in, base);
			} finally {
				in.close();
			}
		}
		Reader reader = new InputStreamReader(in, charset);
		try {
			if (base == null)
				return builder.parse(reader);
			return builder.parse(reader, base);
		} finally {
			reader.close();
		}
	}
}
