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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ReaderTransform extends TransformBuilder {
	private final Reader source;
	private final String systemId;
	private final XMLEventReaderFactory inFactory = XMLEventReaderFactory
			.newInstance();
	private final XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
	private final DocumentFactory builder = DocumentFactory.newInstance();

	public ReaderTransform(Reader source) {
		this.source = source;
		this.systemId = null;
	}

	public ReaderTransform(Reader source, String systemId) {
		this.source = source;
		this.systemId = systemId;
	}

	public void close() throws TransformerException {
		try {
			source.close();
		} catch (IOException e) {
			throw handle(new TransformerException(e));
		}
	}

	@Override
	public Document asDocument() throws TransformerException {
		Reader reader = asReader();
		try {
			try {
				if (systemId == null)
					return builder.parse(reader);
				return builder.parse(reader, systemId);
			} catch (SAXException e) {
				throw handle(new TransformerException(e));
			} catch (ParserConfigurationException e) {
				throw handle(new TransformerException(e));
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			throw handle(new TransformerException(e));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public XMLEventReader asXMLEventReader() throws TransformerException {
		try {
			if (systemId == null)
				return inFactory.createXMLEventReader(source);
			return inFactory.createXMLEventReader(systemId, source);
		} catch (XMLStreamException e) {
			throw handle(new TransformerException(e));
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public InputStream asInputStream() throws TransformerException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
			try {
				toOutputStream(baos);
			} finally {
				baos.close();
			}
			return new ByteArrayInputStream(baos.toByteArray());
		} catch (IOException e) {
			throw handle(new TransformerException(e));
		} catch (TransformerException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public Reader asReader() throws TransformerException {
		return source;
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
	protected void setParameter(String name, Object value) {
		// no parameters
	}

}
