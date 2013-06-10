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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.auth.Group;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.exceptions.NotFound;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.CompletedResponse;
import org.callimachusproject.server.helpers.ResourceTransaction;
import org.callimachusproject.server.helpers.ResponseBuilder;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.RDFObject;

/**
 * Ensures the request is authentic if protected.
 * 
 * @author James Leigh
 * 
 */
public class AuthenticationHandler implements AsyncExecChain {
	private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	private static final String[] PUBLIC_HEADERS = { "Content-Type",
			"Content-Length", "Content-Encoding", "Date", "Server" };
	private static final Set<String> PRIVATE_HEADERS = new HashSet<String>(
			Arrays.asList("set-cookie", "set-cookie2"));
	private final Map<String, AuthorizationManager> managers = new LinkedHashMap<String, AuthorizationManager>();
	private final AsyncExecChain delegate;

	public AuthenticationHandler(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	public synchronized void addOrigin(String origin, CalliRepository repository) {
		AuthorizationService service = AuthorizationService.getInstance();
		AuthorizationManager manager = service.get(repository.getDelegate());
		managers.put(origin, manager);
		manager.resetCache();
	}

	public synchronized void removeOrigin(String origin) {
		managers.remove(origin);
	}

	public synchronized void resetCache() {
		for (AuthorizationManager manager : managers.values()) {
			manager.resetCache();
		}
	}

	@Override
	public Future<CloseableHttpResponse> execute(HttpRoute route,
			HttpRequestWrapper request, HttpContext context,
			HttpExecutionAware execAware,
			FutureCallback<CloseableHttpResponse> callback) throws IOException,
			HttpException {
		CalliContext ctx = CalliContext.adapt(context);
		final InetAddress clientAddr = ctx.getClientAddr();
		final long now = ctx.getReceivedOn();
		final ResourceTransaction trans = ctx.getResourceTransaction();
		final AuthorizationManager manager = getManager(trans);
		try {
			final String allowed = getAllowedOrigin(trans, manager);
			callback = new ResponseCallback(callback) {public void completed(CloseableHttpResponse result) {
				try {
					allow(trans, manager, result, allowed, now, clientAddr);
					super.completed(result);
				} catch (OpenRDFException ex) {
					super.failed(ex);
				} catch (IOException ex) {
					super.failed(ex);
				}
			}};
			String[] requires = trans.getRequires();
			if (requires != null && requires.length == 0) {
				trans.setPublic(true);
			} else {
				RDFObject target = trans.getRequestedResource();
				Set<Group> groups = manager.getAuthorizedParties(target, requires);
				if (manager.isPublic(groups) || groups.isEmpty()
						&& trans.getJavaMethod() == null) {
					trans.setPublic(true);
				} else {
					HttpResponse unauthorized = manager.authorize(trans, groups, now, clientAddr);
					if (unauthorized != null) {
						return new CompletedResponse(callback, new ResponseBuilder(trans).respond(unauthorized));
					}
				}
			}
		} catch (OpenRDFException ex) {
			callback.failed(ex);
		}
		return delegate.execute(route, request, context, execAware, callback);
	}

	private synchronized AuthorizationManager getManager(
			ResourceTransaction request) throws NotFound {
		String origin = request.getOrigin();
		if (managers.containsKey(origin))
			return managers.get(origin);
		if (managers.isEmpty())
			throw new NotFound("Origins not configured");
		return managers.values().iterator().next();
	}

	private String getAllowedOrigin(ResourceTransaction request,
			AuthorizationManager manager) throws OpenRDFException, IOException {
		Set<String> origins = manager.allowOrigin(request);
		if (origins == null || origins.isEmpty())
			return null;
		if (origins.contains("*"))
			return "*";
		String origin = request.getVaryHeader("Origin");
		if (origins.contains(origin))
			return origin;
		StringBuilder sb = new StringBuilder();
		for (String o : origins) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(o);
		}
		return sb.toString();
	}

	void allow(ResourceTransaction request, AuthorizationManager manager, HttpResponse rb,
			String allowedOrigin, long now, InetAddress clientAddr) throws OpenRDFException, IOException {
		if (allowedOrigin != null && !rb.containsHeader(ALLOW_ORIGIN)) {
			rb.setHeader(ALLOW_ORIGIN, allowedOrigin);
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
		HttpMessage msg = manager.authenticationInfo(request, now, clientAddr);
		if (msg != null) {
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
