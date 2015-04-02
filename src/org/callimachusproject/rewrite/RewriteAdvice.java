/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Type;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.fluid.Fluid;
import org.openrdf.http.object.fluid.FluidBuilder;
import org.openrdf.http.object.fluid.FluidException;
import org.openrdf.http.object.fluid.FluidFactory;
import org.openrdf.http.object.fluid.FluidType;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;

public abstract class RewriteAdvice implements Advice {
	private static final Pattern HTTP_HEAD = Pattern
			.compile("(?<!\n)\r?\n(\\S+)\\s*:\\s*(.*)");
	private static final Pattern HTTP_BODY = Pattern
			.compile("\r?\n\r?\n([\\S\\s]+)");
	private final URITemplate[] replacers;
	private final String[] bindingNames;
	private final FluidType[] bindingTypes;
	private final java.lang.reflect.Type returnType;
	private final String[] returnMedia;
	private final TermFactory systemId;

	public RewriteAdvice(String[] bindingNames, FluidType[] bindingTypes,
			URITemplate[] replacers, Method method) {
		this.replacers = replacers;
		this.bindingNames = bindingNames;
		this.bindingTypes = bindingTypes;
		this.returnType = method.getGenericReturnType();
		this.returnMedia = getMediaType(method);
		this.systemId = TermFactory.newInstance(getSystemId(method));
	}

	public Object intercept(ObjectMessage message) throws GatewayTimeout,
			IOException, FluidException, OpenRDFException {
		Object target = message.getTarget();
		String uri;
		ObjectConnection con = null;
		if (target instanceof RDFObject) {
			con = ((RDFObject) target).getObjectConnection();
			uri = ((RDFObject) target).getResource().stringValue();
		} else {
			uri = target.toString();
		}
		HeaderGroup headers = new HeaderGroup();
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		Object[] parameters = message.getParameters();
		String substitute = substitute(uri, getVariables(parameters, uri, fb));
		int n = substitute.indexOf('\n');
		if (n < 0)
			return service(resolve(substitute), headers, null, message, fb).as(
					returnType, returnMedia);
		String location = resolve(substitute.substring(0, n).trim());
		Matcher body = HTTP_BODY.matcher(substitute);
		StringEntity entity = null;
		if (body.find()) {
			entity = new StringEntity(body.group(1), "UTF-8");
		}
		Matcher header = HTTP_HEAD.matcher(substitute);
		while (header.find()) {
			String name = header.group(1);
			String value = header.group(2);
			headers.addHeader(new BasicHeader(name, value));
			if (entity != null) {
				if ("Content-Encoding".equalsIgnoreCase(name)) {
					entity.setContentEncoding(value);
				} else if ("Content-Type".equalsIgnoreCase(name)) {
					entity.setContentType(value);
				}
			}
		}
		return service(location, headers, entity, message, fb).as(returnType,
				returnMedia);
	}

	protected String getSystemId() {
		return systemId.getSystemId();
	}

	protected String[] getReturnMedia() {
		return returnMedia;
	}

	protected abstract Fluid service(String location, HeaderGroup headers,
			HttpEntity entity, ObjectMessage message, FluidBuilder fb)
			throws GatewayTimeout, IOException, FluidException,
			ResponseException, OpenRDFException;

	private String getSystemId(Method m) {
		if (m.isAnnotationPresent(Iri.class))
			return m.getAnnotation(Iri.class).value();
		Class<?> dclass = m.getDeclaringClass();
		String mame = m.getName();
		if (dclass.isAnnotationPresent(Iri.class)) {
			String url = dclass.getAnnotation(Iri.class).value();
			if (url.indexOf('#') >= 0)
				return url.substring(0, url.indexOf('#') + 1) + mame;
			return url + "#" + mame;
		}
		String name = dclass.getSimpleName() + ".class";
		URL url = dclass.getResource(name);
		if (url != null)
			return url.toExternalForm() + "#" + mame;
		return "java:" + dclass.getName() + "#" + mame;
	}

	private String[] getMediaType(Method method) {
		if (method.isAnnotationPresent(Type.class))
			return method.getAnnotation(Type.class).value();
		return new String[0];
	}

	private Map<String, String> getVariables(Object[] parameters, String uri,
			FluidBuilder fb) throws IOException, FluidException {
		if (bindingNames == null || bindingNames.length == 0)
			return Collections.singletonMap("this", uri);
		Map<String, String> map = new HashMap<String, String>(
				bindingNames.length + 1);
		map.put("this", uri);
		for (int i = 0; i < bindingNames.length; i++) {
			String key = bindingNames[i];
			if (key != null) {
				FluidType type = bindingTypes[i];
				Object param = parameters[i];
				if (param != null) {
					map.put(key, asString(param, type, uri, fb));
				}
			}
		}
		return map;
	}

	private String asString(Object param, FluidType type, String uri,
			FluidBuilder fb) throws IOException, FluidException {
		return (String) fb.consume(param, uri, type).as(String.class);
	}

	private String substitute(String uri, Map<String, String> variables) {
		if (replacers == null || replacers.length <= 0)
			return null;
		for (URITemplate template : replacers) {
			CharSequence result = template.process(variables);
			if (result != null)
				return result.toString();
		}
		return null;
	}

	private String resolve(String path) throws UnsupportedEncodingException {
		if (path == null)
			return null;
		String uri = PercentCodec.encodeOthers(path, PercentCodec.ALLOWED);
		return systemId.resolve(uri);
	}

	private Header[] readHeaders(String[] lines) {
		List<Header> headers = new ArrayList<Header>();
		for (int i = 1; i < lines.length; i++) {
			int colon = lines[i].indexOf(':');
			if (colon > 0) {
				String name = lines[i].substring(0, colon).trim();
				String value = lines[i].substring(colon + 1).trim();
				headers.add(new BasicHeader(name, value));
			}
		}
		return headers.toArray(new Header[headers.size()]);
	}

}
