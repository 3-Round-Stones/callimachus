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
package org.callimachusproject.server.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.repository.object.xslt.DocumentFactory;
import org.openrdf.repository.object.xslt.XMLSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * Parses a DocumentFragment from an InputStream.
 */
public class DocumentFragmentMessageReader implements
		MessageBodyReader<DocumentFragment> {
	private static XMLSourceFactory sourceFactory = XMLSourceFactory.newInstance();

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

	public boolean isReadable(MessageType mtype) {
		Class<?> type = mtype.clas();
		String mediaType = mtype.getMimeType();
		if (mediaType != null && !mediaType.startsWith("application/")
				&& !mediaType.contains("xml"))
			return false;
		return type.isAssignableFrom(DocumentFragment.class);
	}

	public DocumentFragment readFrom(MessageType mtype, ReadableByteChannel in,
			Charset charset, String base, String location)
			throws TransformerConfigurationException, TransformerException,
			ParserConfigurationException, IOException, XMLStreamException {
		Class<?> type = mtype.clas();
		if (in == null)
			return null;
		try {
			DocumentFragment node = createNode(type);
			DOMResult result = new DOMResult(node);
			Source source = createSource(location, in, charset);
			Transformer transformer = factory.newTransformer();
			ErrorCatcher listener = new ErrorCatcher();
			transformer.setErrorListener(listener);
			transformer.transform(source, result);
			if (listener.isFatal())
				throw listener.getFatalError();
			return node;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private DocumentFragment createNode(Class<?> type) throws ParserConfigurationException {
		Document doc = builder.newDocument();
		return doc.createDocumentFragment();
	}

	private Source createSource(String location, ReadableByteChannel cin,
			Charset charset) throws TransformerException {
		InputStream in = ChannelUtil.newInputStream(cin);
		if (charset == null && in != null && location != null)
			return sourceFactory.createSource(in, location);
		if (charset == null && in != null && location == null)
			return sourceFactory.createSource(in);
		if (in == null && location != null)
			return sourceFactory.createSource(location);
		Reader reader = new InputStreamReader(in, charset);
		if (location != null)
			return sourceFactory.createSource(reader, location);
		return sourceFactory.createSource(reader);
	}
}
