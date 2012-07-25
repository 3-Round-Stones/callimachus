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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

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
 * Writes a percent encoded form from a {@link Map}.
 * 
 * @author James Leigh
 * 
 */
public class FormMapMessageWriter implements
		Consumer<Map<String, Object>> {

	public boolean isConsumable(FluidType mtype, FluidBuilder delegate) {
		String mimeType = mtype.getMediaType();
		if (!mtype.isMap())
			return false;
		FluidType kt = mtype.getKeyGenericType();
		if (!kt.isUnknown()) {
			if (!delegate.isConsumable(mtype.key("text/plain")))
				return false;
		}
		FluidType vt = mtype.component("text/plain");
		if (vt.isSetOrArray()) {
			if (!delegate.isConsumable(vt.component()))
				return false;
		} else if (!vt.isUnknown()) {
			if (!delegate.isConsumable(vt))
				return false;
		}
		return mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("application/*")
				|| mimeType.startsWith("application/x-www-form-urlencoded");
	}

	private String getMediaType(FluidType mtype, FluidBuilder builder) {
		return "application/x-www-form-urlencoded";
	}

	public Fluid consume(final FluidType ftype, final Map<String, Object> result, final String base,
			final FluidBuilder builder) {
		return new AbstractFluid(builder) {
			public HttpEntity asHttpEntity(String media) throws IOException,
					OpenRDFException, XMLStreamException, TransformerException,
					ParserConfigurationException {
				String mediaType = getMediaType(ftype.as(media), builder);
				return new ReadableHttpEntityChannel(mediaType, -1, write(ftype.as(mediaType), result, base, builder));
			}

			public String toString() {
				return result.toString();
			}
		};
	}

	private ReadableByteChannel write(FluidType mtype,
			Map<String, Object> result, String base, FluidBuilder builder)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		writeTo(mtype, result, base, builder, out, 1024);
		return ChannelUtil.newChannel(out.toByteArray());
	}

	private void writeTo(FluidType mtype, Map<String, Object> result,
			String base, FluidBuilder builder, OutputStream out, int bufSize)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		Charset charset = mtype.getCharset();
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		FluidType vtype = mtype.component("text/plain");
		if (vtype.isUnknown()) {
			vtype = vtype.as(String[].class);
		}
		FluidType vctype = vtype;
		if (vctype.isSetOrArray()) {
			vctype = vctype.component();
		}
		Writer writer = new OutputStreamWriter(out, charset);
		try {
			if (result == null)
				return;
			boolean first = true;
			for (Map.Entry<String, Object> e : result.entrySet()) {
				if (e.getKey() != null) {
					String name = enc(writeTo(mtype.key("text/plain"), e
							.getKey(), base, builder));
					Iterator<?> iter = vtype.iteratorOf(e.getValue());
					if (first) {
						first = false;
					} else {
						writer.append("&");
					}
					if (!iter.hasNext()) {
						writer.append(name);
					}
					while (iter.hasNext()) {
						writer.append("&").append(name);
						Object value = iter.next();
						if (value != null) {
							String str = writeTo(vctype, value, base, builder);
							writer.append("=").append(enc(str));
						}
					}
				}
			}
		} finally {
			writer.close();
		}
	}

	private String enc(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, "UTF-8");
	}

	private String writeTo(FluidType mtype, Object value, String base, FluidBuilder delegate)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		Charset cs = Charset.forName("ISO-8859-1");
		if (mtype.isUnknown() && value != null) {
			mtype = mtype.as(value.getClass());
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		ReadableByteChannel in = delegate.consume(mtype, value, base).asChannel("text/plain;charset=ISO-8859-1");
		try {
			ChannelUtil.transfer(in, out);
		} finally {
			in.close();
		}
		return out.toString("ISO-8859-1");
	}
}
