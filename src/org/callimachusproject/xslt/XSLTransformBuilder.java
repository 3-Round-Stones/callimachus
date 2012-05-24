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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * Helper class to run XSLT with parameters.
 * 
 * @author James Leigh
 */
public class XSLTransformBuilder extends TransformBuilder {
	private Source source;
	private final CloseableURIResolver opened;
	private Transformer transformer;
	private ErrorCatcher listener;
	private DocumentFactory builder = DocumentFactory.newInstance();
	private final DOMSourceFactory sourceFactory = DOMSourceFactory
			.newInstance();

	public XSLTransformBuilder(Transformer transformer, final Source source,
			final URIResolver resolver) throws TransformerException,
			IOException {
		try {
			this.source = source;
			this.transformer = transformer;
			listener = new ErrorCatcher(source.getSystemId());
			opened = new CloseableURIResolver(resolver);
			transformer.setErrorListener(listener);
			transformer.setURIResolver(opened);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public void close() throws IOException, TransformerException {
		sourceFactory.close(source);
		opened.close();
	}

	protected void fatalError(TransformerException exception) {
		listener.fatalError(exception);
	}

	protected void ioException(IOException exception) {
		listener.ioException(exception);
	}

	public DocumentFragment asDocumentFragment() throws TransformerException,
			IOException {
		try {
			Document doc = builder.newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			DOMResult output = new DOMResult(frag);
			transform(output);
			if (output.getNode().hasChildNodes())
				return frag;
			return null;
		} catch (IOException e) {
			throw handle(e);
		} catch (ParserConfigurationException e) {
			throw handle(new TransformerException(e));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public Document asDocument() throws TransformerException, IOException {
		try {
			Document doc = builder.newDocument();
			DOMResult output = new DOMResult(doc);
			transform(output);
			if (listener.isFatal())
				throw listener.getFatalError();
			if (listener.isIOException())
				throw listener.getIOException();
			if (output.getNode().hasChildNodes())
				return doc;
			return null;
		} catch (IOException e) {
			throw handle(e);
		} catch (ParserConfigurationException e) {
			throw handle(new TransformerException(e));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public XMLEventReader asXMLEventReader() throws TransformerException,
			IOException {
		XMLEventReaderFactory infactory = XMLEventReaderFactory.newInstance();
		try {
			Reader reader = asReader();
			if (reader == null)
				return null;
			Properties oformat = new Properties();
			oformat.put("method", "xml");
			transformer.setOutputProperties(oformat);
			return infactory.createXMLEventReader(reader);
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

	public InputStream asInputStream() throws TransformerException, IOException {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
			toOutputStream(output);
			ByteArrayInputStream input = new ByteArrayInputStream(
					output.toByteArray());
			BufferedInputStream buffer = new BufferedInputStream(input);
			ByteBuffer buf = ByteBuffer.allocate(200);
			buffer.mark(buf.limit());
			while (buf.hasRemaining()) {
				int read = buffer.read(buf.array(), buf.position(),
						buf.remaining());
				if (read < 0)
					break;
				buf.position(buf.position() + read);
			}
			if (buf.hasRemaining() && isEmpty(buf.array(), buf.position())) {
				input.close();
				return null;
			}
			buffer.reset();
			return buffer;
		} catch (IOException e) {
			throw handle(e);
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public Reader asReader() throws TransformerException, IOException {
		try {
			CharArrayWriter writer = new CharArrayWriter(8192);
			toWriter(writer);
			CharArrayReader reader = new CharArrayReader(writer.toCharArray());
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
		} catch (IOException e) {
			throw handle(e);
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public void toOutputStream(OutputStream out) throws IOException,
			TransformerException {
		try {
			transform(new StreamResult(out));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	public void toWriter(Writer writer) throws IOException,
			TransformerException {
		try {
			try {
				if (listener.isFatal())
					throw listener.getFatalError();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
						"yes");
				transformer.transform(source, new StreamResult(writer));
				if (listener.isFatal())
					throw listener.getFatalError();
				if (listener.isIOException())
					throw listener.getIOException();
			} catch (TransformerException e) {
				throw e;
			} finally {
				close();
			}
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	protected void setParameter(String name, Object value) {
		if (value != null && transformer != null) {
			transformer.setParameter(name, value);
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

	private void transform(Result result) throws IOException,
			TransformerException {
		try {
			if (listener.isFatal())
				throw listener.getFatalError();
			transformer.transform(source, result);
			if (listener.isFatal())
				throw listener.getFatalError();
			if (listener.isIOException())
				throw listener.getIOException();
		} catch (TransformerException e) {
			throw e;
		} finally {
			close();
		}
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
