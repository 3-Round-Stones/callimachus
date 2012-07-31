/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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
package org.callimachusproject.xslt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.xml.DocumentFactory;
import org.openrdf.OpenRDFException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Helper class to run XSLT with parameters.
 * 
 * @author James Leigh
 */
public abstract class TransformBuilder {
	private final XMLOutputFactory factory = XMLOutputFactory.newInstance();
	private final DocumentFactory builder = DocumentFactory.newInstance();

	public final TransformBuilder with(String name, String value)
			throws TransformerException, IOException {
		return with(name, value, String.class, "text/plain");
	}

	public final TransformBuilder with(String name, Object value)
			throws TransformerException, IOException {
		if (value == null)
			return this;
		return with(name, value, value.getClass(), "text/plain");
	}

	public final TransformBuilder with(String name, Object value, Type valueType, String... media)
			throws TransformerException, IOException {
		try {
			if (value == null)
				return this;
			FluidType ftype = new FluidType(valueType, media);
			if (ftype.isXML()) {
				Document doc = document(value, ftype);
				setParameter(name, new DOMSource(doc));
			} else {
				setParameter(name, String.valueOf(value));
			}
			return this;
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	private Document document(Object value, FluidType ftype)
			throws IOException, TransformerException {
		FluidBuilder fb = FluidFactory.getInstance().builder();
		try {
			return fb.consume(value, null, ftype).asDocument();
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	public final String asString() throws TransformerException, IOException {
		try {
			CharSequence seq = asCharSequence();
			if (seq == null)
				return null;
			return seq.toString();
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public final CharSequence asCharSequence() throws TransformerException,
			IOException {
		try {
			StringWriter output = new StringWriter();
			try {
				toWriter(output);
			} catch (IOException e) {
				throw handle(e);
			}
			StringBuffer buffer = output.getBuffer();
			if (buffer.length() < 100 && isEmpty(buffer.toString()))
				return null;
			return buffer;
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public final Readable asReadable() throws TransformerException, IOException {
		return asReader();
	}

	public final byte[] asByteArray() throws TransformerException, IOException {
		try {
			ByteArrayOutputStream output = asByteArrayOutputStream();
			if (output == null)
				return null;
			return output.toByteArray();
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public final ReadableByteChannel asReadableByteChannel()
			throws TransformerException, IOException {
		try {
			InputStream input = asInputStream();
			if (input == null)
				return null;
			return Channels.newChannel(input);
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public final ByteArrayOutputStream asByteArrayOutputStream()
			throws TransformerException, IOException {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
			toOutputStream(output);
			if (output.size() < 200
					&& isEmpty(output.toByteArray(), output.size()))
				return null;
			return output;
		} catch (TransformerException e) {
			throw handle(e);
		} catch (IOException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public final CharArrayWriter asCharArrayWriter()
			throws TransformerException, IOException {
		try {
			CharArrayWriter output = new CharArrayWriter(8192);
			toWriter(output);
			if (output.size() < 200) {
				byte[] bytes = new String(output.toCharArray())
						.getBytes("UTF-8");
				if (isEmpty(bytes, bytes.length))
					return null;
			}
			return output;
		} catch (TransformerException e) {
			throw handle(e);
		} catch (IOException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public final Object asObject() throws TransformerException, IOException {
		return asDocumentFragment();
	}

	public final Node asNode() throws TransformerException, IOException {
		return asDocument();
	}

	public final Element asElement() throws TransformerException, IOException {
		Document doc = asDocument();
		if (doc == null)
			return null;
		return doc.getDocumentElement();
	}

	public abstract DocumentFragment asDocumentFragment() throws TransformerException,
			IOException;

	public abstract Document asDocument() throws TransformerException,
			IOException;

	public abstract XMLEventReader asXMLEventReader()
			throws TransformerException, IOException;

	public abstract InputStream asInputStream() throws TransformerException,
			IOException;

	public abstract Reader asReader() throws TransformerException, IOException;

	public abstract void toOutputStream(OutputStream out) throws IOException,
			TransformerException;

	public abstract void toWriter(Writer writer) throws IOException,
			TransformerException;

	public abstract void close() throws TransformerException, IOException;

	protected final <E extends Throwable> E handle(E cause)
			throws TransformerException, IOException {
		try {
			close();
			return cause;
		} catch (IOException e) {
			e.initCause(cause);
			throw e;
		} catch (TransformerException e) {
			e.initCause(cause);
			throw e;
		} catch (RuntimeException e) {
			e.initCause(cause);
			throw e;
		} catch (Error e) {
			e.initCause(cause);
			throw e;
		}
	}

	protected void setParameter(String name, Object value) {
		// no parameters
	}

	private NodeList parse(Set<?> values) throws TransformerException,
			IOException {
		try {
			if (values == null)
				return null;
			Document doc = builder.newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			for (Object value : values) {
				Node node = null;
				if (value instanceof Document) {
					node = ((Document) value).getDocumentElement();
				} else if (value instanceof Node) {
					node = (Node) value;
				} else if (value instanceof NodeList) {
					NodeList list = (NodeList) value;
					for (int i = list.getLength() - 1; i >= 0; i--) {
						frag.appendChild(doc.importNode(list.item(i), true));
					}
				} else if (value instanceof ReadableByteChannel) {
					node = parse((ReadableByteChannel) value)
							.getDocumentElement();
				} else if (value instanceof ByteArrayOutputStream) {
					node = parse((ByteArrayOutputStream) value)
							.getDocumentElement();
				} else if (value instanceof XMLEventReader) {
					node = parse((XMLEventReader) value).getDocumentElement();
				} else if (value instanceof InputStream) {
					node = parse((InputStream) value).getDocumentElement();
				} else if (value instanceof Reader) {
					node = parse((Reader) value).getDocumentElement();
				} else if (value instanceof Set) {
					NodeList list = parse((Set<?>) value);
					for (int i = list.getLength() - 1; i >= 0; i--) {
						frag.appendChild(doc.importNode(list.item(i), true));
					}
				} else {
					frag.appendChild(doc.createTextNode(value.toString()));
				}
				if (node != null) {
					frag.appendChild(doc.importNode(node, true));
				}
			}
			return frag.getChildNodes();
		} catch (ParserConfigurationException e) {
			throw handle(new TransformerException(e));
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	private Document parse(byte[] value) throws TransformerException,
			IOException {
		try {
			if (value == null)
				return null;
			return parse(new ByteArrayInputStream(value));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	private Document parse(ReadableByteChannel value)
			throws TransformerException, IOException {
		try {
			if (value == null)
				return null;
			return parse(Channels.newInputStream(value));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	private Document parse(ByteArrayOutputStream value)
			throws TransformerException, IOException {
		try {
			if (value == null)
				return null;
			return parse(value.toByteArray());
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	private Document parse(XMLEventReader value) throws TransformerException,
			IOException {
		try {
			if (value == null)
				return null;
			ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
			XMLEventWriter writer = factory.createXMLEventWriter(output);
			try {
				writer.add(value);
			} finally {
				value.close();
				writer.close();
				output.close();
			}
			return parse(output);
		} catch (IOException e) {
			throw handle(e);
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	private Document parse(InputStream value) throws TransformerException,
			IOException {
		try {
			if (value == null)
				return null;
			try {
				return builder.parse(value);
			} finally {
				value.close();
			}
		} catch (SAXException e) {
			throw handle(new TransformerException(e));
		} catch (ParserConfigurationException e) {
			throw handle(new TransformerException(e));
		} catch (IOException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	private Document parse(Reader value) throws TransformerException,
			IOException {
		try {
			if (value == null)
				return null;
			try {
				return builder.parse(value);
			} finally {
				value.close();
			}
		} catch (SAXException e) {
			throw handle(new TransformerException(e));
		} catch (ParserConfigurationException e) {
			throw handle(new TransformerException(e));
		} catch (IOException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
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
