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
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.model.AsyncExecChain;
import org.callimachusproject.server.model.CalliContext;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.ResponseCallback;
import org.callimachusproject.server.util.HTTPDateFormat;

/**
 * Adds the HTTP headers: Cache-Control, Vary, ETag, Content-Type,
 * Content-Encoding, and Last-Modified.
 * 
 * @author James Leigh
 * 
 */
public class ContentHeadersHandler implements AsyncExecChain {
	private final AsyncExecChain delegate;
	private final HTTPDateFormat format = new HTTPDateFormat();

	public ContentHeadersHandler(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<CloseableHttpResponse> execute(HttpRoute route,
			final HttpRequestWrapper request, HttpContext context,
			HttpExecutionAware execAware,
			FutureCallback<CloseableHttpResponse> callback) throws IOException,
			HttpException {
		final ResourceOperation trans = CalliContext.adapt(context)
				.getResourceTransaction();
		final String contentType = trans.getResponseContentType();
		final String derived = trans.getContentVersion();
		final String cache = trans.getResponseCacheControl();
		return delegate.execute(route, request, context, execAware,
				new ResponseCallback(callback) {
					public void completed(CloseableHttpResponse result) {
						addHeaders(request, trans, contentType, derived, cache, result);
						super.completed(result);
					}
				});
	}

	void addHeaders(HttpRequest req, ResourceOperation trans, String contentType,
			String derived, String cache, HttpResponse rb) {
		String version = trans.isSafe() ? derived : trans.getContentVersion();
		String entityTag = trans.getEntityTag(req, version, cache, contentType);
		long lastModified = trans.getLastModified();
		if (cache != null) {
			rb.setHeader("Cache-Control", cache);
		}
		for (String vary : trans.getVary()) {
			if (!vary.equalsIgnoreCase("Authorization") && !vary.equalsIgnoreCase("Cookie")) {
				rb.addHeader("Vary", vary);
			}
		}
		if (version != null && !rb.containsHeader("Content-Version")) {
			rb.setHeader("Content-Version", "\"" + version + "\"");
		}
		if (derived != null && !derived.equals(version)
				&& !rb.containsHeader("Derived-From")) {
			rb.setHeader("Derived-From", "\"" + derived + "\"");
		}
		if (entityTag != null && !rb.containsHeader("ETag")) {
			rb.setHeader("ETag", entityTag);
		}
		if (contentType != null && rb.getEntity() != null) {
			rb.setHeader("Content-Type", contentType);
		}
		if (lastModified > 0) {
			rb.setHeader("Last-Modified", format.format(lastModified));
		}
	}

}
