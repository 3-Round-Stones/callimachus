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
package org.callimachusproject.server.behaviours;

import info.aduna.net.ParsedURI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.http.HttpResponse;
import org.callimachusproject.server.annotations.expect;
import org.callimachusproject.server.annotations.header;
import org.callimachusproject.server.annotations.method;
import org.callimachusproject.server.annotations.query;
import org.callimachusproject.server.annotations.type;
import org.callimachusproject.server.client.RemoteConnection;
import org.callimachusproject.server.client.SecureSocketAddress;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.server.readers.FormMapMessageReader;
import org.callimachusproject.server.traits.ProxyObject;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.MessageType;
import org.callimachusproject.server.writers.AggregateWriter;
import org.openrdf.annotations.ParameterTypes;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.traits.ObjectMessage;

/**
 * Redirects method calls to method with @method or @operation over HTTP to
 * their object IRI authority.
 * 
 * @author James Leigh
 * 
 */
public abstract class ProxyObjectSupport implements ProxyObject, RDFObject {
	static final String GET_PROXY_ADDRESS = "getProxyObjectInetAddress";
	private static final Map<ObjectConnection, String> local = new WeakHashMap<ObjectConnection, String>();
	private AggregateWriter writer = AggregateWriter.getInstance();
	private InetSocketAddress addr;

	public void setLocalAuthority(String auth) {
		ObjectConnection key = getObjectConnection();
		synchronized (local) {
			local.put(key, auth);
		}
	}

	public String getLocalAuthority() {
		ObjectConnection key = getObjectConnection();
		synchronized (local) {
			return local.get(key);
		}
	}

	@ParameterTypes( {})
	public InetSocketAddress getProxyObjectInetAddress(ObjectMessage msg) {
		if (addr != null)
			return addr;
		InetSocketAddress inet = (InetSocketAddress) msg.proceed();
		if (inet != null)
			return addr = inet;
		if (isLocalResource())
			return null;
		String uri = getResource().stringValue();
		if (!uri.startsWith("http"))
			return null;
		ParsedURI parsed = new ParsedURI(uri);
		String authority = parsed.getAuthority();
		if (authority == null)
			return null;
		if (authority.contains("@")) {
			authority = authority.substring(authority.indexOf('@') + 1);
		}
		String hostname = authority;
		if (hostname.contains(":")) {
			hostname = hostname.substring(0, hostname.indexOf(':'));
		}
		int port = -1;
		if (authority.contains(":")) {
			int idx = authority.indexOf(':') + 1;
			port = Integer.parseInt(authority.substring(idx));
		}
		if ("http".equalsIgnoreCase(parsed.getScheme())) {
			port = port > 0 ? port : 80;
			return addr = new InetSocketAddress(hostname, port);
		} else if ("https".equalsIgnoreCase(parsed.getScheme())) {
			port = port > 0 ? port : 443;
			return addr = new SecureSocketAddress(hostname, port);
		} else {
			return null;
		}
	}

	public Object invokeRemote(Method method, Object[] parameters)
			throws Exception {
		String rm = getRequestMethod(method, parameters);
		String uri = getResource().stringValue();
		String qs = getQueryString(method, parameters);
		Annotation[][] panns = method.getParameterAnnotations();
		int body = getRequestBodyParameterIndex(panns, parameters);
		assert body < 0 || !method.isAnnotationPresent(ParameterTypes.class);
		InetSocketAddress addr = getProxyObjectInetAddress();
		RemoteConnection con = openConnection(addr, rm, qs);
		Map<String, List<String>> headers = getHeaders(method, parameters);
		for (Map.Entry<String, List<String>> e : headers.entrySet()) {
			for (String value : e.getValue()) {
				con.addHeader(e.getKey(), value);
			}
		}
		String accept = getAcceptHeader(method, con.getEnvelopeType());
		if (accept != null && !headers.containsKey("accept")) {
			con.addHeader("Accept", accept);
		}
		if (body >= 0 && parameters[body] != null) {
			Object result = parameters[body];
			Class<?> ptype = method.getParameterTypes()[body];
			Type gtype = method.getGenericParameterTypes()[body];
			if (!Set.class.equals(ptype) || !((Set) result).isEmpty()) {
				String media = getParameterMediaType(panns[body], ptype, gtype);
				con.write(media, ptype, gtype, result);
			}
		}
		int status = con.getResponseCode();
		Class<?> rtype = method.getReturnType();
		if (body < 0 && Set.class.equals(rtype) && status == 404) {
			con.close();
			Type gtype = method.getGenericReturnType();
			Set values = new HashSet();
			ObjectConnection oc = getObjectConnection();
			Annotation[] manns = method.getAnnotations();
			String media = getParameterMediaType(manns, rtype, gtype);
			return new RemoteSetSupport(addr, uri, qs, media, gtype, values, oc);
		} else if (body < 0 && status == 404) {
			con.close();
			return null;
		} else if (status >= 400) {
			try {
				throw ResponseException.create(con.getHttpResponse(), con.toString());
			} finally {
				con.close();
			}
		} else if (Void.TYPE.equals(rtype)) {
			con.close();
			return null;
		} else if (body < 0 && Set.class.equals(rtype)) {
			Type gtype = method.getGenericReturnType();
			Set values = new HashSet((Set) con.read(gtype, rtype));
			ObjectConnection oc = getObjectConnection();
			Annotation[] manns = method.getAnnotations();
			String media = getParameterMediaType(manns, rtype, gtype);
			return new RemoteSetSupport(addr, uri, qs, media, gtype, values, oc);
		} else if (rtype.isAssignableFrom(HttpResponse.class)) {
			if (method.isAnnotationPresent(type.class)) {
				String[] types = method.getAnnotation(type.class).value();
				for (String type : types) {
					if (type.equals(con.getEnvelopeType())) {
						return rtype.cast(con.getHttpResponse());
					}
				}
			}
			return con.read(method.getGenericReturnType(), rtype);
		} else {
			return con.read(method.getGenericReturnType(), rtype);
		}
	}

	private boolean isLocalResource() {
		String auth = URI.create(getResource().stringValue()).getAuthority();
		return auth == null || auth.equals(getLocalAuthority());
	}

	private Map<String, List<String>> getHeaders(Method method, Object[] param)
			throws Exception {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		Annotation[][] panns = method.getParameterAnnotations();
		Class<?>[] ptypes = method.getParameterTypes();
		Type[] gtypes = method.getGenericParameterTypes();
		for (int i = 0; i < param.length && i < panns.length; i++) {
			if (param[i] == null)
				continue;
			for (Annotation ann : panns[i]) {
				if (ann.annotationType().equals(header.class)) {
					Charset cs = Charset.forName("ISO-8859-1");
					String m = getParameterMediaType(panns[i], ptypes[i],
							gtypes[i]);
					String txt = m == null ? "text/plain" : m;
					String value = writeToString(txt, ptypes[i], gtypes[i],
							param[i], cs);
					for (String name : ((header) ann).value()) {
						List<String> list = map.get(name.toLowerCase());
						if (list == null) {
							map.put(name.toLowerCase(),
									list = new LinkedList<String>());
						}
						list.add(value);
					}
				}
			}
		}
		if (method.isAnnotationPresent(header.class)) {
			String[] values = method.getAnnotation(header.class).value();
			for (String header : values) {
				int idx = header.indexOf(':');
				if (idx > 0) {
					String key = header.substring(0, idx).toLowerCase();
					String value = header.substring(idx + 1);
					List<String> list = map.get(key);
					if (list == null) {
						map.put(key, list = new LinkedList<String>());
					}
					list.add(value);
				}
			}
		}
		if (method.isAnnotationPresent(expect.class)) {
			String[] values = method.getAnnotation(expect.class).value();
			if (values.length > 0 && values[0] != null) {
				List<String> list = map.get("expect");
				if (list == null) {
					map.put("expect", list = new LinkedList<String>());
				}
				list.add(values[0]);
			}
		}
		return map;
	}

	private RemoteConnection openConnection(InetSocketAddress addr,
			String method, String qs) throws IOException {
		String uri = getResource().stringValue();
		ObjectConnection oc = getObjectConnection();
		return new RemoteConnection(addr, method, uri, qs, oc);
	}

	private String getRequestMethod(Method method, Object[] parameters) {
		Class<?> rt = method.getReturnType();
		Annotation[][] panns = method.getParameterAnnotations();
		String rm = getPropertyMethod(rt, panns, parameters);
		if (method.isAnnotationPresent(method.class)) {
			String[] values = method.getAnnotation(method.class).value();
			for (String value : values) {
				if (value.equals(rm))
					return value;
			}
			if (values.length > 0)
				return values[0];
		}
		return rm;
	}

	private String getPropertyMethod(Class<?> rt, Annotation[][] panns,
			Object[] parameters) {
		int body = getRequestBodyParameterIndex(panns, parameters);
		if (!Void.TYPE.equals(rt) && body < 0) {
			return "GET";
		}
		if (Void.TYPE.equals(rt) && body >= 0) {
			if (parameters[body] == null)
				return "DELETE";
			return "PUT";
		}
		return "POST";
	}

	private String getQueryString(Method method, Object[] param)
			throws Exception {
		Map<String, String[]> map = new LinkedHashMap<String, String[]>();
		Class<?>[] ptypes = method.getParameterTypes();
		Type[] gtypes = method.getGenericParameterTypes();
		Annotation[][] panns = method.getParameterAnnotations();
		for (int i = 0; i < panns.length; i++) {
			if (param[i] == null)
				continue;
			for (Annotation ann : panns[i]) {
				if (query.class.equals(ann.annotationType())) {
					String name = ((query) ann).value()[0];
					append(ptypes[i], gtypes[i], panns[i], name, param[i], map);
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		if (method.isAnnotationPresent(query.class)) {
			String[] values = method.getAnnotation(query.class).value();
			if (values.length > 0) {
				sb.append(enc(values[0]));
			}
		}
		for (String name : map.keySet()) {
			for (String value : map.get(name)) {
				if (sb.length() > 0) {
					sb.append("&");
				}
				sb.append(enc(name)).append("=").append(enc(value));
			}
		}
		if (sb.length() == 0)
			return null;
		return sb.toString();
	}

	private void append(Class<?> ptype, Type gtype, Annotation[] panns,
			String name, Object param, Map<String, String[]> map)
			throws Exception {
		String m = getParameterMediaType(panns, ptype, gtype);
		ObjectConnection con = getObjectConnection();
		MessageType type = new MessageType(m, ptype, gtype, con);
		Class<?> cc = type.getComponentClass();
		Type ctype = type.getComponentType();
		Charset cs = Charset.forName("ISO-8859-1");
		if ("*".equals(name)) {
			String form = m == null ? "application/x-www-form-urlencoded" : m;
			ReadableByteChannel in = write(form, ptype, gtype, param, cs);
			FormMapMessageReader reader = new FormMapMessageReader();
			Map f = reader.readFrom(type.as(Map.class), in, cs, null, null);
			for (Object key : f.keySet()) {
				if (!map.containsKey(key)) {
					map.put((String) key, (String[]) f.get(key));
				}
			}
		} else if (type.isSet()) {
			List<String> values = new ArrayList<String>();
			String txt = m == null ? "text/plain" : m;
			for (Object o : (Set) param) {
				values.add(writeToString(txt, cc, ctype, o, cs));
			}
			map.put(name, values.toArray(new String[values.size()]));
		} else if (type.isArray()) {
			String txt = m == null ? "text/plain" : m;
			int len = Array.getLength(param);
			String[] values = new String[len];
			for (int i = 0; i < len; i++) {
				values[i] = writeToString(txt, cc, ctype, Array.get(param, i),
						cs);
			}
			map.put(name, values);
		} else {
			String txt = m == null ? "text/plain" : m;
			String value = writeToString(txt, ptype, gtype, param, cs);
			map.put(name, new String[] { value });
		}
	}

	private String getParameterMediaType(Annotation[] anns, Class<?> ptype,
			Type gtype) {
		ObjectConnection con = getObjectConnection();
		for (Annotation ann : anns) {
			if (ann.annotationType().equals(type.class)) {
				for (String media : ((type) ann).value()) {
					if (writer.isWriteable(new MessageType(media, ptype, gtype, con)))
						return media;
				}
			}
		}
		return null;
	}

	private ReadableByteChannel write(String mediaType, Class<?> ptype,
			Type gtype, Object result, Charset charset) throws Exception {
		String uri = getResource().stringValue();
		ObjectConnection con = getObjectConnection();
		return writer.write(new MessageType(mediaType, ptype, gtype, con), result, uri, charset);
	}

	private String writeToString(String mediaType, Class<?> ptype, Type gtype,
			Object result, Charset cs) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ReadableByteChannel in = write(mediaType, ptype, gtype, result, cs);
		try {
			ChannelUtil.transfer(in, out);
		} finally {
			in.close();
		}
		return out.toString(cs.name());
	}

	private String enc(String value) throws AssertionError {
		try {
			return URLEncoder.encode(value, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private String getAcceptHeader(Method method, String ignore) {
		if (method.isAnnotationPresent(type.class)) {
			String[] types = method.getAnnotation(type.class).value();
			if (types.length == 1 && types[0].equals(ignore))
				return "*/*";
			if (types.length == 1)
				return types[0];
			StringBuilder sb = new StringBuilder();
			for (String type : types) {
				if (type.equals(ignore))
					continue;
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(type);
			}
			return sb.toString();
		} else {
			return "*/*";
		}
	}

	private int getRequestBodyParameterIndex(Annotation[][] panns,
			Object[] parameters) {
		for (int i = 0; i < panns.length; i++) {
			boolean body = false;
			for (Annotation ann : panns[i]) {
				if (query.class.equals(ann.annotationType())) {
					body = false;
					break;
				} else if (header.class.equals(ann.annotationType())) {
					body = false;
					break;
				} else if (type.class.equals(ann.annotationType())) {
					body = true;
				}
			}
			if (body && i < parameters.length) {
				return i;
			}
		}
		return -1;
	}

}
