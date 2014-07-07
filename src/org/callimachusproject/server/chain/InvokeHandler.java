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
package org.callimachusproject.server.chain;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.callimachusproject.annotations.expect;
import org.callimachusproject.annotations.header;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.Request;
import org.callimachusproject.server.helpers.ResourceOperation;
import org.callimachusproject.server.helpers.ResponseBuilder;
import org.openrdf.repository.object.traits.RDFObjectBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a Java Method on the request target and response with the result.
 * 
 * @author James Leigh
 * 
 */
public class InvokeHandler implements ClientExecChain {
	private interface MapStringArray extends Map<String, String[]> {
	}
	private interface SetString extends Set<String> {
	}

	private static final Type setOfStringType = SetString.class
			.getGenericInterfaces()[0];

	private static final Type mapOfStringArrayType = MapStringArray.class
			.getGenericInterfaces()[0];
	private final Logger logger = LoggerFactory.getLogger(InvokeHandler.class);

	@Override
	public CloseableHttpResponse execute(HttpRoute route,
			HttpRequestWrapper request, HttpClientContext clientContext,
			HttpExecutionAware execAware) throws IOException, HttpException {
		CalliContext context = CalliContext.adapt(clientContext);
		ResourceOperation trans = context.getResourceTransaction();
		Method method = trans.getJavaMethod();
		assert method != null;
		try {
			return invoke(trans, method, new Request(request, context).isSafe(), new ResponseBuilder(request, context));
		} catch (Error e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (HttpException e) {
			throw e;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	private HttpUriResponse invoke(ResourceOperation req, Method method, boolean safe, ResponseBuilder builder)
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
				String message = e.getMessage();
				if (message == null) {
					message = e.toString();
				}
				return builder.badRequest(message);
			}
			try {
				HttpUriResponse response = invoke(req, method, args, getResponseTypes(req, method), builder);
				if (!safe) {
					req.flush();
				}
				return response;
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
			if (body != null) {
				try {
					body.asVoid();
				} catch (IOException e) {
					logger.error(req.toString(), e);
				}
			}
		}
	}

	private HttpUriResponse invoke(ResourceOperation req, Method method,
			Object[] args, String[] responseTypes, ResponseBuilder rbuilder) throws Exception {
		if (responseTypes == null || responseTypes.length < 1) {
			responseTypes = new String[] { "*/*" };
		}
		Class<?> type = method.getReturnType();
		FluidBuilder builder = req.getFluidBuilder();
		if (!builder.isConsumable(method.getGenericReturnType(), responseTypes))
			throw new NotAcceptable(type.getSimpleName()
					+ " cannot be converted into "
					+ Arrays.asList(responseTypes));

		Object result = method.invoke(req.getRequestedResource(), args);
		if (result instanceof RDFObjectBehaviour) {
			result = ((RDFObjectBehaviour) result).getBehaviourDelegate();
		}
		return createResponse(req, result, method, responseTypes, rbuilder);
	}

	private HttpUriResponse createResponse(ResourceOperation req,
			Object result, Method method, String[] responseTypes, ResponseBuilder rbuilder)
			throws IOException, FluidException {
		int responseCode = 204;
		String responsePhrase = "No Content";
		Set<String> responseLocations = null;
		FluidBuilder builder = req.getFluidBuilder();
		Fluid writer = builder.consume(result, req.getRequestURL(),
				method.getGenericReturnType(), responseTypes);
		Class<?> type = method.getReturnType();
		boolean emptyResult = result == null || Set.class.equals(type)
				&& ((Set<?>) result).isEmpty();
		if (!emptyResult) {
			responseCode = 200;
			responsePhrase = "OK";
		}
		if (method.isAnnotationPresent(expect.class)) {
			for (String expect : method.getAnnotation(expect.class).value()) {
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
							FluidType ftype = new FluidType(setOfStringType,
									"text/uri-list");
							if (writer.toMedia(ftype) != null) {
								responseLocations = (Set<String>) writer
										.as(ftype);
							}
							if (!emptyResult && responseLocations != null
									&& !responseLocations.isEmpty()) {
								responseCode = code;
								responsePhrase = phrase;
								break;
							}
						} else if (code == 204 || code == 205) {
							if (emptyResult) {
								responseCode = code;
								responsePhrase = phrase;
								break;
							}
						} else if (code >= 300 && code <= 399) {
							responseCode = code;
							responsePhrase = phrase;
							FluidType ftype = new FluidType(setOfStringType,
									"text/uri-list");
							if (writer.toMedia(ftype) != null) {
								responseLocations = (Set<String>) writer
										.as(ftype);
							}
							break;
						} else {
							responseCode = code;
							responsePhrase = phrase;
						}
					}
				} catch (NumberFormatException e) {
					logger.error(expect, e);
				} catch (IndexOutOfBoundsException e) {
					logger.error(expect, e);
				}
			}
		}
		HttpUriResponse response = createResponse(req, responseCode,
				responsePhrase, responseLocations, emptyResult ? null : writer,
				rbuilder);
		if (method.isAnnotationPresent(header.class)) {
			for (String header : method.getAnnotation(header.class).value()) {
				int idx = header.indexOf(':');
				if (idx <= 0)
					continue;
				String name = header.substring(0, idx);
				String value = header.substring(idx + 1);
				response.addHeader(name, value);
			}
		}
		return response;
	}

	private HttpUriResponse createResponse(ResourceOperation req,
			int responseCode, String responsePhrase,
			Set<String> responseLocations, Fluid writer, ResponseBuilder builder)
			throws IOException, FluidException {
		HttpUriResponse response;
		if (writer == null) {
			response = builder.noContent(responseCode, responsePhrase);
		} else {
			HttpEntity entity = writer.asHttpEntity(req
					.getResponseContentType());
			response = builder.content(responseCode, responsePhrase, entity);
		}
		if (responseLocations != null && !responseLocations.isEmpty()) {
			for (String location : responseLocations) {
				response.addHeader("Location", location);
			}
		}
		return response;
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

	private Fluid getParameter(ResourceOperation req, Annotation[] anns,
			Class<?> ptype, Fluid input) throws Exception {
		String[] names = req.getParameterNames(anns);
		String[] headers = req.getHeaderNames(anns);
		String[] types = req.getParameterMediaTypes(anns);
		if (names == null && headers == null && types.length == 0) {
			return req.getFluidBuilder().media("*/*");
		} else if (names == null && headers == null) {
			return input;
		} else if (headers != null && names != null) {
			return getHeaderAndQuery(req, headers, names);
		} else if (headers != null) {
			return req.getHeader(headers);
		} else if (names.length == 1 && names[0].equals("*")) {
			return req.getQueryStringParameter();
		} else {
			return getParameter(req, names);
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

}
