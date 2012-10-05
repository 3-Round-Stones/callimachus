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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.callimachusproject.server.auth.AuthorizationManager;
import org.callimachusproject.server.auth.Group;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.Response;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.RDFObject;

/**
 * Ensures the request is authentic if protected.
 * 
 * @author James Leigh
 * 
 */
public class AuthenticationHandler implements Handler {
	private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	private static final String[] PUBLIC_HEADERS = { "Content-Type",
			"Content-Length", "Content-Encoding", "Date", "Server" };
	private static final Set<String> PRIVATE_HEADERS = new HashSet<String>(
			Arrays.asList("set-cookie", "set-cookie2"));
	private final AuthorizationManager manager = AuthorizationManager.getInstance();
	private final Handler delegate;

	public AuthenticationHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public void resetCache() {
		manager.resetCache();
	}

	public Response verify(ResourceOperation request) throws Exception {
		String[] requires = request.getRequires();
		if (requires != null && requires.length == 0) {
			request.setPublic(true);
		} else {
			RDFObject target = request.getRequestedResource();
			Set<Group> groups = manager.getAuthorizedParties(target, requires);
			if (manager.isPublic(groups) || groups.isEmpty() && request.getJavaMethod() == null) {
				request.setPublic(true);
			} else {
				HttpResponse unauthorized = manager.authorize(request, groups);
				if (unauthorized != null) {
					return new Response(allow(request, unauthorized),
							request.getObjectConnection());
				}
			}
		}
		return allow(request, delegate.verify(request));
	}

	public Response handle(ResourceOperation request) throws Exception {
		return allow(request, delegate.handle(request));
	}

	private <R extends HttpMessage> R allow(ResourceOperation request, R rb)
			throws OpenRDFException, IOException {
		if (rb == null)
			return null;
		if (!rb.containsHeader(ALLOW_ORIGIN)) {
			String origins = manager.allowOrigin(request);
			if (origins != null) {
				for (String o : origins.split("[\\s,]+")) {
					if (o.length() > 0) {
						if ("*".equals(o)) {
							rb.setHeader(ALLOW_ORIGIN, o);
							break;
						} else if (o.equals(request.getVaryHeader("Origin"))) {
							rb.setHeader(ALLOW_ORIGIN, o);
							break;
						} else {
							rb.addHeader(ALLOW_ORIGIN, o);
						}
					}
				}
			}
		}
		if (!rb.containsHeader(ALLOW_CREDENTIALS)) {
			String origin = request.getVaryHeader("Origin");
			if (origin != null) {
				if (manager.withAgentCredentials(request, origin)) {
					rb.setHeader(ALLOW_CREDENTIALS, "true");
				} else {
					rb.setHeader(ALLOW_CREDENTIALS, "false");
				}
			}
		}
		HttpMessage msg = manager.authenticationInfo(request);
		if (msg != null) {
			for (Header hd : msg.getAllHeaders()) {
				if (!rb.containsHeader(hd.getName())) {
					rb.setHeader(hd);
				}
			}
		}
		if (!rb.containsHeader(EXPOSE_HEADERS)) {
			String exposed = exposeHeaders(rb);
			if (exposed != null) {
				rb.setHeader(EXPOSE_HEADERS, exposed);
			}
		}
		return rb;
	}

	private String exposeHeaders(HttpMessage rb) {
		Map<String,String> map = new LinkedHashMap<String,String>();
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
