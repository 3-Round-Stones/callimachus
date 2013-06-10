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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.server.model.AsyncExecChain;
import org.callimachusproject.server.model.CalliContext;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.ResponseBuilder;
import org.callimachusproject.server.model.ResponseCallback;

/**
 * Responds for OPTIONS requests.
 * 
 * @author James Leigh
 * 
 */
public class OptionsHandler implements AsyncExecChain {
	private static final String REQUEST_METHOD = "Access-Control-Request-Method";
	private static final Set<String> ALLOW_HEADERS = new TreeSet<String>(
			Arrays.asList("Authorization", "Cache-Control", "Location",
					"Range", "Accept", "Accept-Charset", "Accept-Encoding",
					"Accept-Language", "Content-Encoding", "Content-Language",
					"Content-Length", "Content-Location", "Content-MD5",
					"Content-Type", "If-Match", "If-Modified-Since",
					"If-None-Match", "If-Range", "If-Unmodified-Since"));
	private final AsyncExecChain delegate;

	public OptionsHandler(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<CloseableHttpResponse> execute(HttpRoute route,
			HttpRequestWrapper request, HttpContext context,
			HttpExecutionAware execAware,
			final FutureCallback<CloseableHttpResponse> callback) throws IOException,
			HttpException {
		if ("OPTIONS".equals(request.getRequestLine().getMethod())) {
			ResourceOperation trans = CalliContext.adapt(context).getResourceTransaction();
			StringBuilder sb = new StringBuilder();
			sb.append("OPTIONS, TRACE");
			for (String method : trans.getAllowedMethods()) {
				sb.append(", ").append(method);
			}
			String allow = sb.toString();
			HttpUriResponse rb = new ResponseBuilder(trans).noContent();
			rb.addHeader("Allow", allow);
			String m = trans.getVaryHeader(REQUEST_METHOD);
			if (m == null) {
				rb.addHeader("Access-Control-Allow-Methods", allow);
			} else {
				rb.addHeader("Access-Control-Allow-Methods", m);
			}
			StringBuilder headers = new StringBuilder();
			for (String header : ALLOW_HEADERS) {
				if (headers.length() > 0) {
					headers.append(",");
				}
				headers.append(header);
			}
			for (String header : getAllowedHeaders(m, trans)) {
				if (!ALLOW_HEADERS.contains(header)) {
					headers.append(",");
					headers.append(header);
				}
			}
			rb.addHeader("Access-Control-Allow-Headers", headers.toString());
			String max = getMaxAge(trans.getRequestedResource().getClass());
			if (max != null) {
				rb.addHeader("Access-Control-Max-Age", max);
			}
			BasicFuture<CloseableHttpResponse> future;
			future = new BasicFuture<CloseableHttpResponse>(callback);
			future.completed(rb);
			return future;
		} else {
			return delegate.execute(route, request, context, execAware,
					new ResponseCallback(callback) {
						public void completed(CloseableHttpResponse result) {
							allow(result);
							super.completed(result);
						}
					});
		}
	}

	CloseableHttpResponse allow(CloseableHttpResponse resp) {
		if (resp != null && resp.getStatusLine().getStatusCode() == 405) {
			if (resp.containsHeader("Allow")) {
				String allow = resp.getFirstHeader("Allow").getValue();
				resp.setHeader("Allow", allow + ",OPTIONS");
			} else {
				resp.setHeader("Allow", "OPTIONS");
			}
		}
		return resp;
	}

	private Collection<String> getAllowedHeaders(String m, ResourceOperation request) {
		Collection<String> result = null;
		Class<?> type = request.getRequestedResource().getClass();
		for (Method method : type.getMethods()) {
			if (m != null && method.isAnnotationPresent(method.class)) {
				String[] mm = method.getAnnotation(method.class).value();
				if (!Arrays.asList(mm).contains(m))
					continue;
			}
			for (Annotation[] anns : method.getParameterAnnotations()) {
				for (Annotation ann : anns) {
					if (ann.annotationType().equals(header.class)) {
						if (result == null) {
							result = new TreeSet<String>();
						}
						result.addAll(Arrays.asList(((header) ann).value()));
					}
				}
			}
		}
		if (result == null)
			return Collections.emptyList();
		return result;
	}

	private String getMaxAge(Class<?> type) {
		if (type.isAnnotationPresent(header.class)) {
			for (String value : type.getAnnotation(header.class).value()) {
				int m = value.indexOf("max-age=");
				if (m >= 0) {
					int idx = value.indexOf(':');
					if (idx < 0)
						continue;
					String name = value.substring(0, idx);
					if (!name.equalsIgnoreCase("cache-control"))
						continue;
					int c = value.indexOf(';', m);
					if (c < 0) {
						c = value.length();
					}
					String max = value.substring(m, c);
					return max.trim();
				}
			}
		} else {
			if (type.getSuperclass() != null) {
				String max = getMaxAge(type.getSuperclass());
				if (max != null) {
					return max;
				}
			}
			for (Class<?> face : type.getInterfaces()) {
				String max = getMaxAge(face);
				if (max != null) {
					return max;
				}
			}
		}
		return null;
	}

}
