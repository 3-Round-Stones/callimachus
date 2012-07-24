/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.callimachusproject.fluid.consumers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes text/uri-list files.
 */
public class URIListWriter<URI> implements Consumer<URI> {
	private static final Charset USASCII = Charset.forName("US-ASCII");
	private Logger logger = LoggerFactory.getLogger(URIListWriter.class);
	private StringBodyWriter delegate = new StringBodyWriter();
	private Class<URI> componentType;

	public URIListWriter(Class<URI> componentType) {
		this.componentType = componentType;
	}

	public boolean isText(FluidType mtype) {
		return true;
	}

	public long getSize(FluidType mtype, ObjectConnection con, URI result, Charset charset) {
		String mimeType = mtype.getMediaType();
		Class<?> ctype = mtype.getClassType();
		if (result == null)
			return 0;
		if (Set.class.equals(ctype))
			return -1;
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		if (mtype.isSetOrArray()) {
			if (charset == null) {
				charset = USASCII;
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
			try {
				Writer writer = new OutputStreamWriter(out, charset);
				Iterator<URI> iter = (Iterator<URI>) mtype.iteratorOf(result);
				while (iter.hasNext()) {
					writer.write(toString(iter.next()));
					if (iter.hasNext()) {
						writer.write("\r\n");
					}
				}
				writer.flush();
			} catch (IOException e) {
				logger.error(e.toString(), e);
				return -1;
			}
			return out.size();
		} else {
			Class<String> t = String.class;
			return delegate.getSize(mtype.as(t), con, toString(result), charset);
		}
	}

	public boolean isWriteable(FluidType mtype, ObjectConnection con) {
		Class<?> ctype = mtype.getClassType();
		if (componentType != null) {
			if (!componentType.equals(ctype) && Object.class.equals(ctype))
				return false;
			if (mtype.isSetOrArray()) {
				Class<?> component = mtype.getComponentClass();
				if (!componentType.equals(component)
						&& Object.class.equals(component))
					return false;
				if (!componentType.isAssignableFrom(component)
						&& !component.equals(Object.class))
					return false;
			} else if (!componentType.isAssignableFrom(ctype)) {
				return false;
			}
		}
		return delegate.isWriteable(mtype.as(String.class), con);
	}

	public String getContentType(FluidType mtype, Charset charset) {
		String mimeType = mtype.getMediaType();
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Class<String> t = String.class;
		return delegate.getContentType(mtype.as(t), charset);
	}

	public ReadableByteChannel write(FluidType mtype, ObjectConnection con,
			URI result, String base, Charset charset) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException {
		String mimeType = mtype.getMediaType();
		if (result == null)
			return null;
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		if (mtype.isSetOrArray()) {
			if (charset == null) {
				charset = USASCII;
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
			Writer writer = new OutputStreamWriter(out, charset);
			Iterator<URI> iter = (Iterator<URI>) mtype.iteratorOf(result);
			while (iter.hasNext()) {
				writer.write(toString(iter.next()));
				if (iter.hasNext()) {
					writer.write("\r\n");
				}
			}
			writer.flush();
			return ChannelUtil.newChannel(out.toByteArray());
		} else {
			return delegate.write(mtype.as(String.class), con, toString(result), base, charset);
		}
	}

	public void writeTo(FluidType mtype, URI result, String base,
			Charset charset, OutputStream out, int bufSize) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		if (result == null)
			return;
		String mimeType = mtype.getMediaType();
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		if (mtype.isSetOrArray()) {
			if (charset == null) {
				charset = USASCII;
			}
			Writer writer = new OutputStreamWriter(out, charset);
			Iterator<URI> iter = (Iterator<URI>) mtype.iteratorOf(result);
			while (iter.hasNext()) {
				writer.write(toString(iter.next()));
				if (iter.hasNext()) {
					writer.write("\r\n");
				}
			}
			writer.flush();
		} else {
			delegate.writeTo(mtype.as(String.class),
					toString(result), base, charset, out, bufSize);
		}
	}

	protected String toString(URI result) {
		return result.toString();
	}

}
