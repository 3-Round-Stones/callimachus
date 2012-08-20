/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.XMLEventAllocator;


/**
 * Wraps a XMLInputFactory, but closes input streams when the XMLEventReader is
 * closed. This implementation also ensure every stream starts with
 * {@link javax.xml.stream.events.StartDocument} and ends with
 * {@link javax.xml.stream.events.EndDocument}.
 * 
 * @author James Leigh
 * 
 */
public class XMLEventReaderFactory {

	public static XMLEventReaderFactory newInstance() {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		try {
			factory.setProperty(
					"http://java.sun.com/xml/stream/properties/ignore-external-dtd",
					true);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		return new XMLEventReaderFactory(factory);
	}

	private XMLInputFactory factory;

	public XMLEventReaderFactory(XMLInputFactory factory) {
		this.factory = factory;
	}

	public XMLEventReader createXMLEventReader(InputStream stream,
			Charset charset) throws XMLStreamException {
		try {
			stream = checkForVoidXML(stream);
			if (stream == null)
				return null;
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
		return wrap(factory.createXMLEventReader(stream, charset.name()), stream);
	}

	public XMLEventReader createXMLEventReader(InputStream stream)
			throws XMLStreamException {
		try {
			stream = checkForVoidXML(stream);
			if (stream == null)
				return null;
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
		return wrap(factory.createXMLEventReader(stream), stream);
	}

	public XMLEventReader createXMLEventReader(String systemId,
			InputStream stream) throws XMLStreamException {
		try {
			stream = checkForVoidXML(stream);
			if (stream == null)
				return null;
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
		return wrap(factory.createXMLEventReader(systemId, stream), stream);
	}

	public XMLEventReader createXMLEventReader(Reader reader)
			throws XMLStreamException {
		try {
			reader = checkForVoidXML(reader);
			if (reader == null)
				return null;
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
		return wrap(factory.createXMLEventReader(reader), reader);
	}

	public XMLEventReader createXMLEventReader(String systemId, Reader reader)
			throws XMLStreamException {
		try {
			reader = checkForVoidXML(reader);
			if (reader == null)
				return null;
		} catch (IOException e) {
			throw new XMLStreamException(e);
		}
		return wrap(factory.createXMLEventReader(systemId, reader), reader);
	}

	public XMLEventAllocator getEventAllocator() {
		return factory.getEventAllocator();
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return factory.getProperty(name);
	}

	public XMLReporter getXMLReporter() {
		return factory.getXMLReporter();
	}

	public XMLResolver getXMLResolver() {
		return factory.getXMLResolver();
	}

	public boolean isPropertySupported(String name) {
		return factory.isPropertySupported(name);
	}

	public void setEventAllocator(XMLEventAllocator allocator) {
		factory.setEventAllocator(allocator);
	}

	public void setProperty(String name, Object value)
			throws IllegalArgumentException {
		factory.setProperty(name, value);
	}

	public void setXMLReporter(XMLReporter reporter) {
		factory.setXMLReporter(reporter);
	}

	public void setXMLResolver(XMLResolver resolver) {
		factory.setXMLResolver(resolver);
	}

	private XMLEventReader wrap(XMLEventReader reader, Closeable io) {
		return new DocumentXMLEventReader(new ClosingXMLEventReader(reader, io));
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
		input = new BufferedInputStream(input);
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
