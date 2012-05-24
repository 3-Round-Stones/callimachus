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
package org.callimachusproject.xslt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLEventTransform extends TransformBuilder {
	private XMLEventReader source;
	private final String systemId;
	private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
	private final DocumentFactory builder = DocumentFactory.newInstance();

	public XMLEventTransform(XMLEventReader source, String systemId) {
		this.source = source;
		this.systemId = systemId;
	}

	public void close() throws TransformerException, IOException {
		try {
			source.close();
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		}
	}

	@Override
	public Document asDocument() throws TransformerException, IOException {
		InputStream in = asInputStream();
		try {
			try {
				return builder.parse(in, systemId);
			} catch (SAXException e) {
				throw handle(new TransformerException(e));
			} catch (ParserConfigurationException e) {
				throw handle(new TransformerException(e));
			} finally {
				in.close();
			}
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

	@Override
	public XMLEventReader asXMLEventReader() throws TransformerException,
			IOException {
		return source;
	}

	@Override
	public InputStream asInputStream() throws TransformerException, IOException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
			try {
				toOutputStream(baos);
			} finally {
				baos.close();
			}
			return new ByteArrayInputStream(baos.toByteArray());
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

	@Override
	public Reader asReader() throws TransformerException, IOException {
		try {
			CharArrayWriter caw = new CharArrayWriter(8192);
			try {
				toWriter(caw);
			} catch (IOException e) {
				throw handle(e);
			} finally {
				caw.close();
			}
			return new CharArrayReader(caw.toCharArray());
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public void toOutputStream(OutputStream out) throws IOException,
			TransformerException {
		XMLEventReader reader = asXMLEventReader();
		try {
			try {
				XMLEventWriter writer = outFactory.createXMLEventWriter(out,
						"UTF-8");
				writer.add(reader);
				writer.flush();
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public void toWriter(Writer writer) throws IOException,
			TransformerException {
		XMLEventReader reader = asXMLEventReader();
		try {
			try {
				XMLEventWriter xml = outFactory.createXMLEventWriter(writer);
				xml.add(reader);
				xml.flush();
			} finally {
				reader.close();
			}
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	protected void setParameter(String name, Object value) {
		// no parameters
	}

}
