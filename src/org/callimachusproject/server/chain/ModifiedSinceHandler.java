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

import java.util.Enumeration;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.ResourceOperation;
import org.callimachusproject.server.helpers.ResponseBuilder;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.callimachusproject.server.util.HTTPDateFormat;

/**
 * Response with 304 and 412 when resource has not been modified.
 * 
 * @author James Leigh
 * 
 */
public class ModifiedSinceHandler implements AsyncExecChain {
	private final AsyncExecChain delegate;
	private long reset = System.currentTimeMillis() / 1000 * 1000;
	private HTTPDateFormat format = new HTTPDateFormat();

	public ModifiedSinceHandler(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	public void invalidate() {
		reset = System.currentTimeMillis() / 1000 * 1000;
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target,
			HttpRequest request, HttpContext context,
			FutureCallback<HttpResponse> callback) {
		callback = new ResponseCallback(callback) {
			public void completed(HttpResponse result) {
				try {
					resetModified(result);
					super.completed(result);
				} catch (RuntimeException ex) {
					super.failed(ex);
				}
			}
		};
		ResourceOperation req = CalliContext.adapt(context).getResourceTransaction();
		String method = req.getMethod();
		String contentType = req.getResponseContentType();
		String cache = req.getResponseCacheControl();
		String entityTag = req.getEntityTag(request, req.getContentVersion(), cache, contentType);
		if (req.isSafe() && req.isNoValidate()) {
			return delegate.execute(target, request, context, callback);
		} else {
			HttpUriResponse resp;
			String tag = modifiedSince(req, entityTag);
			if ("GET".equals(method) || "HEAD".equals(method)) {
				if (tag == null) {
					return delegate.execute(target, request, context, callback);
				}
				resp = new ResponseBuilder(request, context).notModified();
			} else if (tag == null) {
				return delegate.execute(target, request, context, callback);
			} else {
				resp = new ResponseBuilder(request, context).preconditionFailed();
			}
			if (tag.length() > 0) {
				resp.setHeader("ETag", tag);
			}
			BasicFuture<HttpResponse> future;
			future = new BasicFuture<HttpResponse>(callback);
			future.completed(resp);
			return future;
		}
	}

	void resetModified(HttpResponse resp) {
		Header lastHeader = resp.getLastHeader("Last-Modified");
		if (reset > 0 && lastHeader != null && reset > format.parseDate(lastHeader.getValue())) {
			resp.setHeader("Last-Modified", format.format(reset));
		}
	}

	private String modifiedSince(ResourceOperation req, String entityTag) {
		long lastModified = Math.max(reset, req.getLastModified());
		boolean notModified = false;
		try {
			if (lastModified > 0) {
				long modified = req.getDateHeader("If-Modified-Since");
				notModified = modified > 0;
				if (notModified && modified < lastModified)
					return null;
			}
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		Enumeration matchs = req.getHeaderEnumeration("If-None-Match");
		boolean mustMatch = matchs.hasMoreElements();
		if (mustMatch) {
			String match = null;
			while (matchs.hasMoreElements()) {
				match = (String) matchs.nextElement();
				if (match(entityTag, match))
					return match;
			}
		}
		if (!notModified || mustMatch)
			return null;
		return "";
	}

	private boolean match(String tag, String match) {
		if (tag == null)
			return false;
		if ("*".equals(match))
			return true;
		if (match.startsWith("W/") && !tag.startsWith("W/")) {
			match = match.substring(2);
		}
		if (match.equals(tag))
			return true;
		int md = match.indexOf('-');
		int td = tag.indexOf('-');
		if (td >= 0 && md >= 0)
			return false;
		if (md < 0) {
			md = match.lastIndexOf('"');
		}
		if (td < 0) {
			td = tag.lastIndexOf('"');
		}
		int mq = match.indexOf('"');
		int tq = tag.indexOf('"');
		if (mq < 0 || tq < 0 || md < 0 || td < 0)
			return false;
		return match.substring(mq, md).equals(tag.substring(tq, td));
	}

}
