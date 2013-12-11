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

import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.ResourceOperation;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.callimachusproject.server.util.HTTPDateFormat;

/**
 * Adds the HTTP headers: Cache-Control, Vary, ETag, Content-Type,
 * and Last-Modified.
 * 
 * @author James Leigh
 * 
 */
public class ContentHeadersFilter implements AsyncExecChain {
	private final AsyncExecChain delegate;
	private final HTTPDateFormat format = new HTTPDateFormat();

	public ContentHeadersFilter(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target,
			final HttpRequest request, final HttpContext context,
			FutureCallback<HttpResponse> callback) {
		final ResourceOperation trans = CalliContext.adapt(context)
				.getResourceTransaction();
		final String contentType = trans.getResponseContentType();
		final String derived = trans.getContentVersion();
		final String cache = trans.getResponseCacheControl();
		return delegate.execute(target, request, context, new ResponseCallback(callback) {
			public void completed(HttpResponse result) {
				try {
					addHeaders(request, context, trans, contentType, derived, cache, result);
					super.completed(result);
				} catch (RuntimeException ex) {
					super.failed(ex);
				}
			}
		});
	}

	void addHeaders(HttpRequest req, HttpContext context,
			ResourceOperation trans, String contentType, String derived,
			String cache, HttpResponse rb) {
		String version = trans.isSafe() ? derived : trans.getContentVersion();
		String entityTag = trans.getEntityTag(req, version, cache, contentType);
		long lastModified = trans.getLastModified();
		int code = rb.getStatusLine().getStatusCode();
		if (code != 412 && code != 304 && trans.isSafe()) {
			StringBuilder sb = new StringBuilder();
			if (cache != null) {
				sb.append(cache);
			}
			if (sb.indexOf("private") < 0 && sb.indexOf("public") < 0) {
				CalliContext ctx = CalliContext.adapt(context);
				if (!ctx.isPublic() && sb.indexOf("s-maxage") < 0) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append("s-maxage=0");
				} else if (ctx.isPublic()) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append("public");
				}
			}
			if (sb.length() > 0) {
				rb.setHeader("Cache-Control", sb.toString());
			}
			for (String vary : trans.getVary()) {
				if (!vary.equalsIgnoreCase("Authorization") && !vary.equalsIgnoreCase("Cookie")) {
					rb.addHeader("Vary", vary);
				}
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
		if (contentType != null && rb.getEntity() != null && !rb.containsHeader("Content-Type")) {
			rb.setHeader("Content-Type", contentType);
		}
		if (lastModified > 0) {
			rb.setHeader("Last-Modified", format.format(lastModified));
		}
	}

}
