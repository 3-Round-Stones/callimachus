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
package org.callimachusproject.fluid.producers;

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

import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Producer;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Readers a percent encoded form into a {@link Map}.
 * 
 * @author James Leigh
 * 
 */
public final class FormMapMessageReader implements
		Producer<Map<String, Object>> {
	private final FluidFactory ff = FluidFactory.getInstance();

	public boolean isReadable(FluidType mtype, ObjectConnection con) {
		Fluid delegate = ff.builder(con).nil("text/plain");
		String mimeType = mtype.getMediaType();
		if (!mtype.isMap())
			return false;
		if (!mtype.key().isUnknown()) {
			if (!delegate.isProducible(mtype.key("text/plain")))
				return false;
		}
		FluidType vt = mtype.component("text/plain");
		if (vt.isSetOrArray()) {
			if (!delegate.isProducible(vt.component("text/plain")))
				return false;
		} else if (!vt.isUnknown()) {
			if (!delegate.isProducible(vt))
				return false;
		}
		return mimeType != null
				&& mimeType.startsWith("application/x-www-form-urlencoded");
	}

	public Map<String, Object> readFrom(FluidType mtype,
			ObjectConnection con, ReadableByteChannel in, Charset charset,
			String base, String location) throws TransformerConfigurationException,
			OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException, TransformerException,
			URISyntaxException {
		try {
			if (charset == null) {
				charset = Charset.forName("ISO-8859-1");
			}
			FluidType vtype = mtype.component("text/plain");
			if (vtype.isUnknown()) {
				vtype = vtype.as(String[].class);
				mtype = mtype.as(new ParameterizedType() {
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
			FluidType ktype = mtype.key("text/plain");
			Class<?> kc = mtype.key().asClass();
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
				Fluid kf = ff.builder(con).channel("text/plain;charset=" + charset.name(), kin, base);
				Object key = kf.produce(ktype.asType(), ktype.getMediaType());
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
					Fluid vf = ff.builder(con).channel("text/plain;charset=" + charset.name(), vin, base);
					if (vtype.isSetOrArray()) {
						FluidType cvtype = vtype.component("text/plain");
						values.add(vf.produce(cvtype));
					} else {
						values.add(vf.produce(vtype));
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
