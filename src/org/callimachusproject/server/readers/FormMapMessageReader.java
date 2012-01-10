/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.callimachusproject.server.readers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

/**
 * Readers a percent encoded form into a {@link Map}.
 * 
 * @author James Leigh
 * 
 */
public final class FormMapMessageReader implements
		MessageBodyReader<Map<String, Object>> {
	private AggregateReader delegate = AggregateReader.getInstance();

	public boolean isReadable(MessageType mtype) {
		String mimeType = mtype.getMimeType();
		if (!mtype.isMap())
			return false;
		MessageType kt = mtype.getKeyGenericType();
		if (!kt.isUnknown()) {
			if (!delegate.isReadable(mtype.key("text/plain")))
				return false;
		}
		MessageType vt = mtype.component("text/plain");
		if (vt.isSetOrArray()) {
			if (!delegate.isReadable(vt.component("text/plain")))
				return false;
		} else if (!vt.isUnknown()) {
			if (!delegate.isReadable(vt))
				return false;
		}
		return mimeType != null
				&& mimeType.startsWith("application/x-www-form-urlencoded");
	}

	public Map<String, Object> readFrom(MessageType mtype,
			ReadableByteChannel in, Charset charset, String base,
			String location) throws TransformerConfigurationException,
			OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException,
			URISyntaxException {
		try {
			if (charset == null) {
				charset = Charset.forName("ISO-8859-1");
			}
			MessageType vtype = mtype.component("text/plain");
			if (vtype.isUnknown()) {
				vtype = vtype.as(String[].class);
				mtype = mtype.as(Map.class, new ParameterizedType() {
					public Type getRawType() {
						return Map.class;
					}

					public Type getOwnerType() {
						return null;
					}

					public Type[] getActualTypeArguments() {
						return new Type[] { String.class, String[].class };
					}
				});
			}
			MessageType ktype = mtype.key("text/plain");
			Class<?> kc = mtype.getKeyClass();
			if (Object.class.equals(kc)) {
				kc = String.class;
				ktype = ktype.as(String.class);
			}
			Map parameters = new LinkedHashMap();
			Scanner scanner = new Scanner(in, charset.name());
			scanner.useDelimiter("&");
			while (scanner.hasNext()) {
				String[] nameValue = scanner.next().split("=", 2);
				if (nameValue.length == 0 || nameValue.length > 2)
					continue;
				String name = decode(nameValue[0]);
				ReadableByteChannel kin = ChannelUtil.newChannel(name
						.getBytes(charset));
				Object key = delegate.readFrom(ktype, kin, charset, base, null);
				if (nameValue.length < 2) {
					if (!parameters.containsKey(key)) {
						parameters.put(key, new ArrayList());
					}
				} else {
					String value = decode(nameValue[1]);
					Collection values = (Collection) parameters.get(key);
					if (values == null) {
						parameters.put(key, values = new ArrayList());
					}
					ReadableByteChannel vin = ChannelUtil.newChannel(value
							.getBytes(charset));
					if (vtype.isSetOrArray()) {
						values.add(delegate.readFrom(vtype
								.component("text/plain"), vin, charset, base,
								null));
					} else {
						values.add(delegate.readFrom(vtype, vin, charset, base,
								null));
					}
				}
			}
			return (Map<String, Object>) mtype.castMap(parameters);
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private String decode(String v) throws UnsupportedEncodingException {
		return URLDecoder.decode(v, "UTF-8");
	}
}
