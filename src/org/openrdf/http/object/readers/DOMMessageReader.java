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
package org.openrdf.http.object.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.MessageType;
import org.openrdf.repository.object.xslt.DocumentFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Parses a DOM Node from an InputStream.
 */
public class DOMMessageReader implements MessageBodyReader<Node> {

	private DocumentFactory builder = DocumentFactory.newInstance();

	public boolean isReadable(MessageType mtype) {
		Class<?> type = mtype.clas();
		String mediaType = mtype.getMimeType();
		if (mediaType != null && !mediaType.startsWith("text/")
				&& !mediaType.startsWith("application/")
				&& !mediaType.contains("xml"))
			return false;
		return type.isAssignableFrom(Document.class)
				|| type.isAssignableFrom(Element.class);
	}

	public Node readFrom(MessageType mtype, ReadableByteChannel cin,
			Charset charset, String base, String location)
			throws TransformerConfigurationException, TransformerException,
			ParserConfigurationException, IOException, SAXException {
		Class<?> type = mtype.clas();
		if (cin == null)
			return null;
		Document doc = createDocument(cin, charset, location);
		if (type.isAssignableFrom(Element.class))
			return doc.getDocumentElement();
		return doc;
	}

	private Document createDocument(ReadableByteChannel cin, Charset charset,
			String location) throws ParserConfigurationException, SAXException,
			IOException {
		try {
			InputStream in = ChannelUtil.newInputStream(cin);
			if (charset == null && in != null && location != null) {
				return builder.parse(in, location);
			}
			if (charset == null && in != null && location == null) {
				return builder.parse(in);
			}
			if (in == null && location != null) {
				return builder.parse(location);
			}
			Reader reader = new InputStreamReader(in, charset);
			InputSource is = new InputSource(reader);
			if (location != null) {
				is.setSystemId(location);
			}
			return builder.parse(is);
		} finally {
			if (cin != null) {
				cin.close();
			}
		}
	}
}
