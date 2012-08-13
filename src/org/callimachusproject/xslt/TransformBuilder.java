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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import javax.xml.stream.XMLEventReader;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * Helper class to run XSLT with parameters.
 * 
 * @author James Leigh
 */
public abstract class TransformBuilder {

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
		} catch (FluidException e) {
			throw new TransformerException(e);
		}
	}

	public String asString() throws TransformerException, IOException {
		return (String) as(String.class);
	}

	public XMLEventReader asXMLEventReader() throws TransformerException, IOException {
		return (XMLEventReader) as(XMLEventReader.class);
	}

	public abstract Object as(Type type, String... media)
			throws TransformerException, IOException;

	public abstract DocumentFragment asDocumentFragment() throws TransformerException,
			IOException;

	public abstract Document asDocument() throws TransformerException,
			IOException;

	public abstract InputStream asInputStream() throws TransformerException,
			IOException;

	public abstract Reader asReader() throws TransformerException, IOException;

	public abstract void toOutputStream(OutputStream out) throws IOException,
			TransformerException;

	public abstract void toWriter(Writer writer) throws IOException,
			TransformerException;

	public abstract void close() throws TransformerException, IOException;

	protected abstract void setParameter(String name, Object value);

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
}
