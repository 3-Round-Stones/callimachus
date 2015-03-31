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
package org.callimachusproject.auth;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.HeaderParam;
import org.openrdf.http.object.chain.HttpRequestChainInterceptor;
import org.openrdf.http.object.helpers.ObjectContext;
import org.openrdf.http.object.helpers.ResourceTarget;
import org.openrdf.repository.object.RDFObject;

/**
 * Ensures the request is authentic if protected.
 * 
 * @author James Leigh
 * 
 */
public class AuthenticationHandler implements HttpRequestChainInterceptor {
	private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	private static final String[] PUBLIC_HEADERS = { "Content-Type",
			"Content-Length", "Content-Encoding", "Date", "Server" };
	private static final Set<String> PRIVATE_HEADERS = new HashSet<String>(
			Arrays.asList("set-cookie", "set-cookie2"));

	@Override
	public HttpResponse intercept(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		try {
			ObjectContext ctx = ObjectContext.adapt(context);
			AuthorizationManager manager = getManager(ctx);
			ResourceTarget resource = ctx.getResourceTarget();
			HttpRequest oreq = ctx.getOriginalRequest();
			String[] requires = getRequires(oreq == null ? request : oreq, resource);
			RDFObject target = resource.getTargetObject();
			Set<Group> groups = manager.getAuthorizedParties(target, requires);
			if (!isPublic(request, manager, resource, requires, groups)) {
				HttpResponse unauthorized = manager.authorize(request, groups, context);
				if (unauthorized != null) {
					return unauthorized;
				}
			}
			return null;
		} catch (OpenRDFException e) {
			throw new AuthenticationException(e.getMessage(), e);
		}
	}

	@Override
	public void process(HttpRequest request, HttpResponse response, HttpContext context)
			throws HttpException, IOException {
		try {
			AuthorizationManager manager = getManager(context);
			String allowed = getAllowedOrigin(request, context, manager);
			allow(request, manager, response, allowed, context);
		} catch (OpenRDFException e) {
			throw new AuthenticationException(e.getMessage(), e);
		}
	}

	void allow(HttpRequest request, AuthorizationManager manager, HttpResponse rb,
			String allowedOrigin, HttpContext context) throws OpenRDFException, IOException {
		if (allowedOrigin != null && !rb.containsHeader(ALLOW_ORIGIN)) {
			rb.setHeader(ALLOW_ORIGIN, allowedOrigin);
		}
		if (!rb.containsHeader(ALLOW_CREDENTIALS)) {
			Header origin = request.getLastHeader("Origin");
			if (origin != null) {
				if (manager.withAgentCredentials(request, context, origin.getValue())) {
					rb.setHeader(ALLOW_CREDENTIALS, "true");
				} else {
					rb.setHeader(ALLOW_CREDENTIALS, "false");
				}
			}
		}
		HttpMessage msg = manager.authenticationInfo(request, context);
		if (msg != null) {
			for (Header hd : msg.getAllHeaders()) {
				rb.removeHeaders(hd.getName());
			}
			for (Header hd : msg.getAllHeaders()) {
				rb.addHeader(hd);
			}
		}
		if (!rb.containsHeader(EXPOSE_HEADERS)) {
			String exposed = exposeHeaders(rb);
			if (exposed != null) {
				rb.setHeader(EXPOSE_HEADERS, exposed);
			}
		}
		int code = rb.getStatusLine().getStatusCode();
		if (code != 412 && code != 304 && isSafe(request)) {
			StringBuilder sb = new StringBuilder();
			if (rb.containsHeader("Cache-Control")) {
				for (Header cache : rb.getHeaders("Cache-Control")) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(cache.getValue());
				}
			}
			if (sb.indexOf("private") < 0 && sb.indexOf("public") < 0) {
				ObjectContext ctx = ObjectContext.adapt(context);
				ResourceTarget resource = ctx.getResourceTarget();
				String[] requires = getRequires(request, resource);
				RDFObject target = resource.getTargetObject();
				Set<Group> groups = manager.getAuthorizedParties(target, requires);
				boolean pub = isPublic(request, manager, resource, requires, groups);
				if (!pub && isPrivate(request, rb, resource)) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append("private");
				} if (!pub && sb.indexOf("s-maxage") < 0) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append("s-maxage=0");
				} else if (pub) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append("public");
				}
			}
			if (sb.length() > 0) {
				rb.setHeader("Cache-Control", sb.toString());
			}
		}
	}

	private AuthorizationManager getManager(
			HttpContext context) throws OpenRDFException, IOException {
		ObjectContext ctx = ObjectContext.adapt(context);
		CalliObject target = (CalliObject) ctx.getResourceTarget().getTargetObject();
		return target.getCalliRepository().getAuthorizationManager();
	}

	private String getAllowedOrigin(HttpRequest request, HttpContext context,
			AuthorizationManager manager) throws OpenRDFException, IOException {
		Set<String> origins = manager.allowOrigin(request, context);
		if (origins == null || origins.isEmpty())
			return null;
		if (origins.contains("*"))
			return "*";
		Header origin = request.getLastHeader("Origin");
		if (origin != null && origins.contains(origin.getValue()))
			return origin.getValue();
		StringBuilder sb = new StringBuilder();
		for (String o : origins) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(o);
		}
		return sb.toString();
	}

	private boolean isSafe(HttpRequest request) {
		String method = request.getRequestLine().getMethod();
		return "GET".equals(method) || "HEAD".equals(method);
	}

	private boolean isPublic(HttpRequest request, AuthorizationManager manager,
			ResourceTarget resource, String[] requires, Set<Group> groups) {
		return requires != null && requires.length == 0
				|| manager.isPublic(groups) || groups.isEmpty()
				&& getHandlerMethod(request, resource) == null;
	}

	private boolean isPrivate(HttpRequest request, HttpResponse response,
			ResourceTarget target) {
		Method method = getHandlerMethod(request, target);
		if (method == null)
			return false;
		for (Annotation[] anns : method.getParameterAnnotations()) {
			for (Annotation ann : anns) {
				if (ann.annotationType().equals(HeaderParam.class)) {
					for (String value : ((HeaderParam) ann).value()) {
						if ("Authorization".equalsIgnoreCase(value)
								|| "Cookie".equalsIgnoreCase(value))
							return true;
					}
				}
			}
		}
		return false;
	}

	private String[] getRequires(HttpRequest request, ResourceTarget target) {
		Method method = getHandlerMethod(request, target);
		if (method == null) {
			Set<String> list = new HashSet<String>();
			for (Method m : target.getTargetObject().getClass().getMethods()) {
				requires ann = m.getAnnotation(requires.class);
				if (ann != null) {
					list.addAll(Arrays.asList(ann.value()));
				}
			}
			return list.toArray(new String[list.size()]);
		}
		requires ann = method.getAnnotation(requires.class);
		if (ann == null)
			return null;
		return ann.value();
	}

	private Method getHandlerMethod(HttpRequest request, ResourceTarget target) {
		Method method = target.getHandlerMethod(request);
		RequestLine line = request.getRequestLine();
		if (method != null || !"HEAD".equals(line.getMethod()))
			return method;
		HttpRequest get = new BasicHttpRequest("GET", line.getUri(), line.getProtocolVersion());
		get.setHeaders(request.getAllHeaders());
		return target.getHandlerMethod(get);
	}

	private String exposeHeaders(HttpMessage rb) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Header hd : rb.getAllHeaders()) {
			String lc = hd.getName().toLowerCase();
			if (!PRIVATE_HEADERS.contains(lc)) {
				map.put(lc, hd.getName());
			}
		}
		for (String name : PUBLIC_HEADERS) {
			String lc = name.toLowerCase();
			if (!PRIVATE_HEADERS.contains(lc)) {
				map.put(lc, name);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String name : map.values()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(name);
		}
		return sb.toString();
	}

}
