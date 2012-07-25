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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;

/**
 * Writes text/uri-list files.
 */
public class URIListWriter<URI> implements Consumer<URI> {
	private static final Charset USASCII = Charset.forName("US-ASCII");
	private StringBodyWriter delegate = new StringBodyWriter();
	private Class<URI> componentType;

	public URIListWriter(Class<URI> componentType) {
		this.componentType = componentType;
	}

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		Class<?> ctype = mtype.asClass();
		if (componentType != null) {
			if (!componentType.equals(ctype) && Object.class.equals(ctype))
				return false;
			if (mtype.isSetOrArray()) {
				Class<?> component = mtype.component().asClass();
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
		return delegate.isConsumable(mtype.as(String.class), builder);
	}

	private String getMediaType(FluidType mtype, FluidBuilder builder) {
		String mimeType = mtype.getMediaType();
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		Charset charset = mtype.getCharset();
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		if (mimeType.contains("charset=") || !mimeType.startsWith("text/"))
			return mimeType;
		return mimeType + ";charset=" + charset.name();
	
	}

	public Fluid consume(final FluidType ftype, final URI result, final String base,
			final FluidBuilder builder) {
		if (result == null)
			return delegate.consume(ftype.as(String.class), null, base, builder);
		if (!ftype.isSetOrArray()) {
			return delegate.consume(ftype.as(String.class), toString(result), base, builder);
		}
		return new AbstractFluid(builder) {
			public HttpEntity asHttpEntity(String media) throws IOException,
					OpenRDFException, XMLStreamException, TransformerException,
					ParserConfigurationException {
				String mediaType = getMediaType(ftype.as(media), builder);
				byte[] buf = write(ftype.as(mediaType), result, base);
				return new ReadableHttpEntityChannel(mediaType, buf.length, ChannelUtil.newChannel(buf));
			}

			public String toString() {
				return result.toString();
			}
		};
	}

	private byte[] write(FluidType mtype, URI result,
			String base) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException {
		String mimeType = mtype.getMediaType();
		if (result == null)
			return null;
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/uri-list";
		}
		return write(mtype, result);
	}

	private byte[] write(FluidType mtype, URI result) throws IOException {
		Charset charset = mtype.getCharset();
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
		return out.toByteArray();
	}

	protected String toString(URI result) {
		return result.toString();
	}

}
