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
package org.callimachusproject.server.writers;

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

import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.OpenRDFException;

/**
 * Writes a percent encoded form from a {@link Map}.
 * 
 * @author James Leigh
 * 
 */
public class FormMapMessageWriter implements
		MessageBodyWriter<Map<String, Object>> {
	private AggregateWriter delegate = AggregateWriter.getInstance();

	public boolean isText(MessageType mtype) {
		return true;
	}

	public long getSize(MessageType mtype, Map<String, Object> result,
			Charset charset) {
		return -1;
	}

	public boolean isWriteable(MessageType mtype) {
		String mimeType = mtype.getMimeType();
		if (!mtype.isMap())
			return false;
		MessageType kt = mtype.getKeyGenericType();
		if (!kt.isUnknown()) {
			if (!delegate.isWriteable(mtype.key("text/plain")))
				return false;
		}
		MessageType vt = mtype.component("text/plain");
		if (vt.isSetOrArray()) {
			if (!delegate.isWriteable(vt.component()))
				return false;
		} else if (!vt.isUnknown()) {
			if (!delegate.isWriteable(vt))
				return false;
		}
		return mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("application/*")
				|| mimeType.startsWith("application/x-www-form-urlencoded");
	}

	public String getContentType(MessageType mtype, Charset charset) {
		return "application/x-www-form-urlencoded";
	}

	public ReadableByteChannel write(MessageType mtype,
			Map<String, Object> result, String base, Charset charset)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		writeTo(mtype, result, base, charset, out, 1024);
		return ChannelUtil.newChannel(out.toByteArray());
	}

	public void writeTo(MessageType mtype, Map<String, Object> result,
			String base, Charset charset, OutputStream out, int bufSize)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		MessageType vtype = mtype.component("text/plain");
		if (vtype.isUnknown()) {
			vtype = vtype.as(String[].class);
		}
		MessageType vctype = vtype;
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
							.getKey(), base));
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
							String str = writeTo(vctype, value, base);
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

	private String writeTo(MessageType mtype, Object value, String base)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		Charset cs = Charset.forName("ISO-8859-1");
		if (mtype.isUnknown() && value != null) {
			mtype = mtype.as(value.getClass());
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		ReadableByteChannel in = delegate.write(mtype, value, base, cs);
		try {
			ChannelUtil.transfer(in, out);
		} finally {
			in.close();
		}
		return out.toString("ISO-8859-1");
	}
}
