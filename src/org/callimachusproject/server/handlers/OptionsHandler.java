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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.Response;

/**
 * Responds for OPTIONS requests.
 * 
 * @author James Leigh
 * 
 */
public class OptionsHandler implements Handler {
	private static final String REQUEST_METHOD = "Access-Control-Request-Method";
	private static final String ALLOW_HEADERS = "Authorization,Host,Cache-Control,Location,Range,"
			+ "Accept,Accept-Charset,Accept-Encoding,Accept-Language,"
			+ "Content-Encoding,Content-Language,Content-Length,Content-Location,Content-MD5,Content-Type,"
			+ "If-Match,If-Modified-Since,If-None-Match,If-Range,If-Unmodified-Since";
	private final Handler delegate;

	public OptionsHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public Response verify(ResourceOperation request) throws Exception {
		if ("OPTIONS".equals(request.getMethod()))
			return null;
		return allow(delegate.verify(request));
	}

	public Response handle(ResourceOperation request) throws Exception {
		if ("OPTIONS".equals(request.getMethod())) {
			StringBuilder sb = new StringBuilder();
			sb.append("OPTIONS, TRACE");
			for (String method : request.getAllowedMethods()) {
				sb.append(", ").append(method);
			}
			String allow = sb.toString();
			Response rb = new Response();
			rb = rb.header("Allow", allow);
			String m = request.getVaryHeader(REQUEST_METHOD);
			if (m == null) {
				rb = rb.header("Access-Control-Allow-Methods", allow);
			} else {
				rb = rb.header("Access-Control-Allow-Methods", m);
			}
			StringBuilder headers = new StringBuilder();
			headers.append(ALLOW_HEADERS);
			for (String header : getAllowedHeaders(m, request)) {
				headers.append(",");
				headers.append(header);
			}
			rb = rb.header("Access-Control-Allow-Headers", headers.toString());
			String max = getMaxAge(request.getRequestedResource().getClass());
			if (max != null) {
				rb = rb.header("Access-Control-Max-Age", max);
			}
			return rb;
		} else {
			return allow(delegate.handle(request));
		}
	}

	private Response allow(Response resp) {
		if (resp != null && resp.getStatusCode() == 405) {
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
		List<String> result = null;
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
							result = new ArrayList<String>();
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
