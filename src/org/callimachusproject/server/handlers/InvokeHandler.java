/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.handlers;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.callimachusproject.annotations.expect;
import org.callimachusproject.annotations.header;
import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.Response;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a Java Method on the request target and response with the result.
 * 
 * @author James Leigh
 * 
 */
public class InvokeHandler implements Handler {
	private interface MapStringArray extends Map<String, String[]> {
	}

	private static Type mapOfStringArrayType = MapStringArray.class
			.getGenericInterfaces()[0];
	private Logger logger = LoggerFactory.getLogger(InvokeHandler.class);

	public Response verify(ResourceOperation request) throws Exception {
		Method method = request.getJavaMethod();
		assert method != null;
		return null;
	}

	public Response handle(ResourceOperation request) throws Exception {
		Method method = request.getJavaMethod();
		assert method != null;
		return invoke(request, method, request.isSafe());
	}

	private Response invoke(ResourceOperation req, Method method, boolean safe)
			throws Exception {
		Fluid body = req.getBody();
		try {
			Object[] args;
			try {
				args = getParameters(req, method, body);
			} catch (ParserConfigurationException e) {
				throw e;
			} catch (TransformerConfigurationException e) {
				throw e;
			} catch (Exception e) {
				return new Response().badRequest(e);
			}
			try {
				ResponseFluid entity = invoke(req, method, args, true,
						getResponseTypes(req, method));
				if (!safe) {
					req.flush();
				}
				return createResponse(req, method, entity);
			} finally {
				for (Object arg : args) {
					if (arg instanceof Closeable) {
						((Closeable) arg).close();
					}
				}
			}
		} catch (InvocationTargetException e) {
			try {
				throw e.getCause();
			} catch (Error cause) {
				throw cause;
			} catch (Exception cause) {
				throw cause;
			} catch (Throwable cause) {
				throw e;
			}
		} finally {
			body.asVoid();
		}
	}

	private ResponseFluid invoke(ResourceOperation req, Method method,
			Object[] args, boolean follow, String... responseTypes)
			throws Exception {
		Object result = method.invoke(req.getRequestedResource(), args);
		ResponseFluid input = createResultEntity(req, result,
				method.getReturnType(), method.getGenericReturnType(),
				responseTypes);
		if (method.isAnnotationPresent(header.class)) {
			for (String header : method.getAnnotation(header.class).value()) {
				int idx = header.indexOf(':');
				if (idx <= 0)
					continue;
				String name = header.substring(0, idx);
				String value = header.substring(idx + 1);
				input.addHeader(name, value);
			}
		}
		if (method.isAnnotationPresent(expect.class)) {
			input.addExpects(method.getAnnotation(expect.class).value());
		}
		if (follow) {
			Method transform = req.getBestTransformMethod(method);
			if (transform != null && !transform.equals(method)) {
				ResponseFluid ret = invoke(req, transform,
						getParameters(req, transform, input), follow,
						getResponseTypes(req, transform));
				ret.addHeaders(input.getOtherHeaders());
				ret.addExpects(input.getExpects());
				return ret;
			}
		}
		return input;
	}

	private String[] getResponseTypes(ResourceOperation req, Method method) {
		String preferred = req.getContentType(method);
		String[] types = req.getTypes(method);
		if (preferred == null)
			return types;
		String[] result = new String[types.length + 1];
		result[0] = preferred;
		System.arraycopy(types, 0, result, 1, types.length);
		return result;
	}

	private Fluid getValue(ResourceOperation req, Annotation[] anns,
			Fluid input, boolean typeRequired) throws Exception {
		for (String uri : req.getTransforms(anns)) {
			Method transform = req.getTransform(uri);
			if (!req.getReadableTypes(input, transform, 0, typeRequired)
					.isEmpty()) {
				Object[] args = getParameters(req, transform, input);
				return invoke(req, transform, args, false,
						req.getTypes(transform));
			}
		}
		return input;
	}

	private Fluid getParameter(ResourceOperation req, Annotation[] anns,
			Class<?> ptype, Fluid input) throws Exception {
		String[] names = req.getParameterNames(anns);
		String[] headers = req.getHeaderNames(anns);
		String[] types = req.getParameterMediaTypes(anns);
		if (names == null && headers == null && types.length == 0) {
			return getValue(req, anns, req.getFluidBuilder().media("*/*"),
					false);
		} else if (names == null && headers == null) {
			return getValue(req, anns, input, true);
		} else if (headers != null && names != null) {
			return getValue(req, anns, getHeaderAndQuery(req, headers, names),
					true);
		} else if (headers != null) {
			return getValue(req, anns, req.getHeader(headers), true);
		} else if (names.length == 1 && names[0].equals("*")) {
			return getValue(req, anns, req.getQueryStringParameter(), true);
		} else {
			return getValue(req, anns, getParameter(req, names), true);
		}
	}

	private Fluid getHeaderAndQuery(ResourceOperation req, String[] headers,
			String[] queries) {
		String[] qvalues = getParameterValues(req, queries);
		if (qvalues == null)
			return req.getHeader(headers);
		List<String> hvalues = req.getVaryHeaders(headers);
		int size = qvalues.length + hvalues.size();
		List<String> list = new ArrayList<String>(size);
		if (qvalues.length > 0) {
			list.addAll(Arrays.asList(qvalues));
		}
		list.addAll(hvalues);
		String[] values = list.toArray(new String[list.size()]);
		FluidType ftype = new FluidType(String[].class, "text/plain", "text/*");
		FluidBuilder fb = req.getFluidBuilder();
		return fb.consume(values, req.getIRI(), ftype);
	}

	private Fluid getParameter(ResourceOperation req, String... names) {
		String[] values = getParameterValues(req, names);
		FluidType ftype = new FluidType(String[].class, "text/plain", "text/*");
		FluidBuilder fb = req.getFluidBuilder();
		return fb.consume(values, req.getIRI(), ftype);
	}

	private String[] getParameterValues(ResourceOperation req, String... names) {
		if (names.length == 0) {
			return new String[0];
		} else {
			Map<String, String[]> map = getParameterMap(req);
			if (map == null) {
				return null;
			} else if (names.length == 1) {
				return map.get(names[0]);
			} else {
				List<String> list = new ArrayList<String>(names.length * 2);
				for (String name : names) {
					list.addAll(Arrays.asList(map.get(name)));
				}
				return list.toArray(new String[list.size()]);
			}
		}
	}

	public Map<String, String[]> getParameterMap(ResourceOperation req) {
		try {
			return (Map<String, String[]>) req.getQueryStringParameter().as(
					new FluidType(mapOfStringArrayType,
							"application/x-www-form-urlencoded"));
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	private Object[] getParameters(ResourceOperation req, Method method,
			Fluid input) throws Exception {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			Fluid entity = getParameter(req, anns[i], ptypes[i], input);
			if (entity != null) {
				String[] types = req.getParameterMediaTypes(anns[i]);
				args[i] = entity.as(new FluidType(gtypes[i], types));
			}
		}
		return args;
	}

	private ResponseFluid createResultEntity(ResourceOperation req,
			Object result, Class<?> ctype, Type gtype, String[] mimeTypes) {
		if (result instanceof RDFObjectBehaviour) {
			result = ((RDFObjectBehaviour) result).getBehaviourDelegate();
		}
		return new ResponseFluid(mimeTypes, result, ctype, gtype, req.getIRI(),
				req.getObjectConnection());
	}

	public Response createResponse(ResourceOperation req, Method method,
			ResponseFluid resp) throws Exception {
		Response rb;
		if (resp.isNoContent()) {
			rb = new Response().noContent();
		} else {
			rb = new Response(resp.asHttpEntity(req.getResponseContentType()));
		}
		for (Map.Entry<String, String> e : resp.getOtherHeaders().entrySet()) {
			rb.header(e.getKey(), e.getValue());
		}
		for (String expect : resp.getExpects()) {
			String[] values = expect.split("[\\s\\-]+");
			try {
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i < values.length; i++) {
					sb.append(values[i].substring(0, 1).toUpperCase());
					sb.append(values[i].substring(1));
					if (i < values.length - 1) {
						sb.append(" ");
					}
				}
				if (sb.length() > 1) {
					int code = Integer.parseInt(values[0]);
					String phrase = sb.toString();
					if (code >= 300 && code <= 303 || code == 307
							|| code == 201) {
						Set<String> locations = resp.getLocations();
						if (locations != null && !locations.isEmpty()) {
							rb = rb.status(code, phrase);
							for (String location : locations) {
								rb.header("Location", location);
							}
							break;
						}
					} else if (code == 204 || code == 205) {
						if (resp.isNoContent()) {
							rb = rb.status(code, phrase);
							break;
						}
					} else if (code >= 300 && code <= 399) {
						rb = rb.status(code, phrase);
						Set<String> locations = resp.getLocations();
						if (locations != null && !locations.isEmpty()) {
							for (String location : locations) {
								rb = rb.header("Location", location);
							}
						}
						break;
					} else {
						rb = rb.status(code, phrase);
					}
				}
			} catch (NumberFormatException e) {
				logger.error(expect, e);
			} catch (IndexOutOfBoundsException e) {
				logger.error(expect, e);
			}
		}
		return rb;
	}

	/**
	 * Wraps a message response to output to an HTTP response.
	 */
	private static class ResponseFluid extends AbstractFluid {
		private interface SetString extends Set<String> {
		}

		private static Type setOfStringType = SetString.class
				.getGenericInterfaces()[0];
		private final FluidFactory ff = FluidFactory.getInstance();
		private final Fluid writer;
		private final Object result;
		private final Class<?> type;
		private final Map<String, String> headers = new HashMap<String, String>();
		private final List<String> expects = new ArrayList<String>();

		public ResponseFluid(String[] mimeTypes, Object result, Class<?> type,
				Type genericType, String base, ObjectConnection con) {
			this.result = result;
			this.type = type;
			if (mimeTypes == null || mimeTypes.length < 1) {
				mimeTypes = new String[] { "*/*" };
			}
			FluidBuilder builder = ff.builder(con);
			if (!builder.isConsumable(genericType, mimeTypes))
				throw new NotAcceptable(type.getSimpleName()
						+ " cannot be converted into " + mimeTypes);
			this.writer = builder.consume(result, base, genericType, mimeTypes);
		}

		public String toString() {
			return String.valueOf(writer);
		}

		public boolean isNoContent() {
			return result == null || Set.class.equals(type)
					&& ((Set<?>) result).isEmpty();
		}

		public Set<String> getLocations() throws IOException, FluidException {
			FluidType ftype = new FluidType(setOfStringType, "text/uri-list");
			if (writer.toMedia(ftype) == null)
				return null;
			return (Set<String>) writer.as(ftype);
		}

		public Map<String, String> getOtherHeaders() {
			return headers;
		}

		public void addHeaders(Map<String, String> map) {
			for (Map.Entry<String, String> e : map.entrySet()) {
				addHeader(e.getKey(), e.getValue());
			}
		}

		public void addHeader(String name, String value) {
			if (headers.containsKey(name)) {
				String string = headers.get(name);
				if (!string.equals(value)) {
					headers.put(name, string + ',' + value);
				}
			} else {
				headers.put(name, value);
			}
		}

		public List<String> getExpects() {
			return expects;
		}

		public void addExpects(String... expects) {
			addExpects(Arrays.asList(expects));
		}

		public void addExpects(List<String> expects) {
			this.expects.addAll(expects);
		}

		@Override
		public FluidType getFluidType() {
			return writer.getFluidType();
		}

		@Override
		public String getSystemId() {
			return writer.getSystemId();
		}

		@Override
		public void asVoid() throws IOException, FluidException {
			writer.asVoid();
		}

		@Override
		public String toMedia(FluidType ftype) {
			return writer.toMedia(ftype);
		}

		@Override
		public Object as(FluidType ftype) throws IOException, FluidException {
			if (writer.toMedia(ftype) == null)
				throw new ClassCastException(String.valueOf(result)
						+ " cannot be converted into " + ftype);
			return writer.as(ftype);
		}
	}

}
