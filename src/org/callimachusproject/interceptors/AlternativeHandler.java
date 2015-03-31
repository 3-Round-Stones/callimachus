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
package org.callimachusproject.interceptors;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.annotations.rel;
import org.openrdf.annotations.Path;
import org.openrdf.http.object.chain.HttpRequestChainInterceptor;
import org.openrdf.http.object.helpers.ObjectContext;
import org.openrdf.http.object.helpers.ResourceTarget;
import org.openrdf.repository.object.RDFObject;

/**
 * If a GET request cannot be satisfied send a redirect to another operation.
 * 
 * @author James Leigh
 * 
 */
public class AlternativeHandler implements HttpRequestChainInterceptor {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final StatusLine _302 = new BasicStatusLine(HttpVersion.HTTP_1_1, 302, "Found");
	private static final StatusLine _303 = new BasicStatusLine(HttpVersion.HTTP_1_1, 303, "See Other");
	private final Pattern SIMPLE = Pattern
			.compile("^\\??([^\\.\\$\\|\\(\\)\\[\\{\\^\\?\\*\\+\\\\]|\\\\[^a-zA-Z0-9])*$");

	@Override
	public HttpResponse intercept(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		String rm = request.getRequestLine().getMethod();
		if (!"GET".equals(rm) && !"HEAD".equals(rm))
			return null; // not safe
		ObjectContext ctx = ObjectContext.adapt(context);
		String uri = request.getRequestLine().getUri();
		ResourceTarget resource = ctx.getResourceTarget();
		RDFObject target = resource.getTargetObject();
		String iri = target.getResource().stringValue();
		if (!iri.endsWith(uri))
			return null; // not requesting this resource directly
		if (ctx.getResourceTarget().getHandlerMethod(request) != null)
			return null; // handler exists
		if ("HEAD".equals(rm) && ctx.getResourceTarget().getHandlerMethod(asGetRequest(request)) != null)
			return null; // GET handler exists
		Iterable<Method> methods = findRelMethods(target.getClass().getMethods());
		String alternate = findRelSuffix(request, "alternate", resource, methods);
		if (alternate != null) {
			BasicHttpResponse response = new BasicHttpResponse(_302);
			response.addHeader("Location", iri + alternate);
			response.setEntity(new StringEntity(alternate, ContentType.create("text/uri-list", UTF8)));
			return response;
		}
		String describedby = findRelSuffix(request, "describedby", resource, methods);
		if (describedby != null){
			BasicHttpResponse response = new BasicHttpResponse(_303);
			response.addHeader("Location", iri + describedby);
			response.setEntity(new StringEntity(alternate, ContentType.create("text/uri-list", UTF8)));
			return response;
		}
		return null;
	}

	public void process(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		// no-op
	}

	private HttpRequest asGetRequest(HttpRequest request) {
		RequestLine line = request.getRequestLine();
		ProtocolVersion ver = line.getProtocolVersion();
		BasicHttpRequest get = new BasicHttpRequest("GET", line.getUri(), ver);
		get.setHeaders(request.getAllHeaders());
		return get;
	}

	private Iterable<Method> findRelMethods(Method[] methods) {
		List<Method> result = new ArrayList<Method>();
		for (Method method : methods) {
			rel rel = method.getAnnotation(rel.class);
			if (rel == null)
				continue;
			org.openrdf.annotations.Method m = method.getAnnotation(org.openrdf.annotations.Method.class);
			if (m == null || !Arrays.asList(m.value()).contains("GET"))
				continue;
			Path p = method.getAnnotation(Path.class);
			if (p == null)
				continue;
			result.add(method);
		}
		return result;
	}

	private String findRelSuffix(HttpRequest request, String rel,
			ResourceTarget resource, Iterable<Method> methods) {
		for (Method method : methods) {
			rel r = method.getAnnotation(rel.class);
			if (r == null)
				continue;
			Path p = method.getAnnotation(Path.class);
			if (p == null)
				continue;
			for (String value : r.value()) {
				if (!rel.equals(value))
					continue;
				for (String regex : p.value()) {
					if (SIMPLE.matcher(regex).matches()) {
						String suffix = regex.replace("\\", "");
						HttpRequest alt = addSuffix(request, suffix);
						if (method.equals(resource.getHandlerMethod(alt)))
							return suffix;
					}
				}
			}
		}
		return null;
	}

	private HttpRequest addSuffix(HttpRequest request, String suffix) {
		HttpRequest req = new BasicHttpRequest("GET", request.getRequestLine().getUri() + suffix);
		req.setHeaders(request.getAllHeaders());
		return req;
	}

}
