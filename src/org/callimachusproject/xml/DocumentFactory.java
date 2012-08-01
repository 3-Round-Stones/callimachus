/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.xml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DocumentFactory {
	private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	private static final Logger logger = LoggerFactory
			.getLogger(DocumentFactory.class);

	public static DocumentFactory newInstance() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setIgnoringComments(false);
		factory.setIgnoringElementContentWhitespace(false);
		try {
			factory.setFeature(LOAD_EXTERNAL_DTD, false);
		} catch (ParserConfigurationException e) {
			logger.warn(e.toString(), e);
		}
		return new DocumentFactory(factory);
	}

	private final DocumentBuilderFactory factory;

	protected DocumentFactory(DocumentBuilderFactory builder) {
		this.factory = builder;
	}

	public Document newDocument() throws ParserConfigurationException {
		return factory.newDocumentBuilder().newDocument();
	}

	public Document parse(InputStream in, String systemId) throws SAXException,
			IOException, ParserConfigurationException {
		in = checkForVoidXML(in);
		if (in == null)
			return null;
		return factory.newDocumentBuilder().parse(in, systemId);
	}

	public Document parse(InputStream in) throws SAXException, IOException,
			ParserConfigurationException {
		in = checkForVoidXML(in);
		if (in == null)
			return null;
		return factory.newDocumentBuilder().parse(in);
	}

	public Document parse(Reader reader, String systemId) throws SAXException,
			IOException, ParserConfigurationException {
		reader = checkForVoidXML(reader);
		if (reader == null)
			return null;
		InputSource is = new InputSource(reader);
		is.setSystemId(systemId);
		return factory.newDocumentBuilder().parse(is);
	}

	public Document parse(Reader reader) throws SAXException, IOException,
			ParserConfigurationException {
		reader = checkForVoidXML(reader);
		if (reader == null)
			return null;
		return factory.newDocumentBuilder().parse(new InputSource(reader));
	}

	private Reader checkForVoidXML(Reader reader) throws IOException {
		if (reader == null)
			return null;
		if (!reader.markSupported()) {
			reader = new BufferedReader(reader);
		}
		CharBuffer cbuf = CharBuffer.allocate(100);
		reader.mark(cbuf.limit());
		while (cbuf.hasRemaining()) {
			int read = reader.read(cbuf);
			if (read < 0)
				break;
		}
		if (cbuf.hasRemaining() && isEmpty(cbuf.flip().toString())) {
			reader.close();
			return null;
		}
		reader.reset();
		return reader;
	}

	private InputStream checkForVoidXML(InputStream input) throws IOException {
		if (input == null)
			return null;
		if (!input.markSupported()) {
			input = new BufferedInputStream(input);
		}
		ByteBuffer buf = ByteBuffer.allocate(200);
		input.mark(buf.limit());
		while (buf.hasRemaining()) {
			int read = input.read(buf.array(), buf.position(),
					buf.remaining());
			if (read < 0)
				break;
			buf.position(buf.position() + read);
		}
		if (buf.hasRemaining() && isEmpty(buf.array(), buf.position())) {
			input.close();
			return null;
		}
		input.reset();
		return input;
	}

	private boolean isEmpty(byte[] buf, int len) {
		if (len == 0)
			return true;
		String xml = decodeXML(buf, len);
		if (xml == null)
			return false; // Don't start with < in UTF-8 or UTF-16
		return isEmpty(xml);
	}

	private boolean isEmpty(String xml) {
		if (xml == null || xml.length() < 1 || xml.trim().length() < 1)
			return true;
		if (xml.length() < 2)
			return false;
		if (xml.charAt(0) != '<' || xml.charAt(1) != '?')
			return false;
		if (xml.charAt(xml.length() - 2) != '?'
				|| xml.charAt(xml.length() - 1) != '>')
			return false;
		for (int i = 1, n = xml.length() - 2; i < n; i++) {
			if (xml.charAt(i) == '<')
				return false;
		}
		return true;
	}

	/**
	 * Decodes the stream just enough to read the &lt;?xml declaration. This
	 * method can distinguish between UTF-16, UTF-8, and EBCDIC xml files, but
	 * not UTF-32.
	 * 
	 * @return a string starting with &lt; or null
	 */
	private String decodeXML(byte[] buf, int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append((char) buf[i]);
		}
		String s = sb.toString();
		String APPFcharset = null; // 'charset' according to XML APP. F
		int byteOrderMark = 0;
		if (s.startsWith("\u00FE\u00FF")) {
			APPFcharset = "UTF-16BE";
			byteOrderMark = 2;
		} else if (s.startsWith("\u00FF\u00FE")) {
			APPFcharset = "UTF-16LE";
			byteOrderMark = 2;
		} else if (s.startsWith("\u00EF\u00BB\u00BF")) {
			APPFcharset = "UTF-8";
			byteOrderMark = 3;
		} else if (s.startsWith("\u0000<")) {
			APPFcharset = "UTF-16BE";
		} else if (s.startsWith("<\u0000")) {
			APPFcharset = "UTF-16LE";
		} else if (s.startsWith("<")) {
			APPFcharset = "US-ASCII";
		} else if (s.startsWith("\u004C\u006F\u00A7\u0094")) {
			APPFcharset = "CP037"; // EBCDIC
		} else {
			return null;
		}
		try {
			byte[] bytes = s.substring(byteOrderMark).getBytes("iso-8859-1");
			String xml = new String(bytes, APPFcharset);
			if (xml.startsWith("<"))
				return xml;
			return null;
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

}
