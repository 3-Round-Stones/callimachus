/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.CloseableEntity;
import org.callimachusproject.client.GUnzipEntity;
import org.callimachusproject.client.GZipEntity;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.Request;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uncompresses the response if the requesting client does not explicitly say it
 * accepts gzip.
 */
public class GUnzipFilter implements AsyncExecChain {
	private static final BasicStatusLine STATUS_203 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 203, "Non-Authoritative Information");
	private static final String hostname = DomainNameSystemResolver.getInstance().getLocalHostName();
	private static final String WARN_214 = "214 " + hostname
			+ " \"Transformation applied\"";
	private final Logger logger = LoggerFactory.getLogger(GUnzipFilter.class);
	private final AsyncExecChain delegate;

	public GUnzipFilter(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target,
			HttpRequest request, final HttpContext context,
			FutureCallback<HttpResponse> callback) {
		Header hd = request.getFirstHeader("Content-Encoding");
		if (hd != null && "gzip".equals(hd.getValue())) {
			Request req = new Request(request, context);
			HttpEntity entity = req.getEntity();
			if (entity != null) {
				// Keep original MD5 (if present) for digest auth
				req.removeHeaders("Content-Length");
				req.setHeader("Content-Encoding", "identity");
				req.setHeader("Transfer-Encoding", "chunked");
				req.addHeader("Warning", WARN_214);
				req.setEntity(gunzip(entity));
				request = req;
			}
		}
		final HttpRequest req = request;
		return delegate.execute(target, request, context, new ResponseCallback(callback) {
			public void completed(HttpResponse result) {
				try {
					filter(req, context, result);
					super.completed(result);
				} catch (RuntimeException ex) {
					super.failed(ex);
				}
			}
		});
	}

	void filter(HttpRequest req, HttpContext context, final HttpResponse resp) {
		Header cache = resp.getFirstHeader("Cache-Control");
		if (cache != null && cache.getValue().contains("no-transform"))
			return;
		Boolean gzip = null;
		boolean encode = false; // gunzip by default
		for (Header header : req.getHeaders("Accept-Encoding")) {
			for (String value : header.getValue().split("\\s*,\\s*")) {
				String[] items = value.split("\\s*;\\s*");
				double q = 1;
				try {
					for (int i = 1; i < items.length; i++) {
						if (items[i].startsWith("q=")) {
							q = Double.parseDouble(items[i].substring(2));
						}
					}
				} catch(NumberFormatException e) {
					logger.warn(e.toString(), e);
				}
				if ("gzip".equals(items[0])) {
					gzip = q > 0;
				} else if ("*".equals(items[0])) {
					encode = q > 0;
				}
			}
		}
		if (gzip == null ? encode : gzip)
			return;
		Header encoding = resp.getFirstHeader("Content-Encoding");
		if (encoding != null && "gzip".equals(encoding.getValue())) {
			HttpEntity entity = resp.getEntity();
			if (entity != null) {
				resp.removeHeaders("Content-MD5");
				resp.removeHeaders("Content-Length");
				resp.setHeader("Content-Encoding", "identity");
				resp.setHeader("Transfer-Encoding", "chunked");
				if (resp.getStatusLine().getStatusCode() == 200) {
					resp.setStatusLine(STATUS_203);
				} else {
					resp.addHeader("Warning", WARN_214);
				}
				if (entity instanceof GZipEntity) {
					resp.setEntity(((GZipEntity) entity).getEntityDelegate());
				} else {
					resp.setEntity(new GUnzipEntity(entity));
				}
			}
		}
		return;
	}

	private HttpEntity gunzip(HttpEntity entity) {
		if (entity instanceof GZipEntity)
			return ((GZipEntity) entity).getEntityDelegate();
		if (entity instanceof CloseableEntity) {
			CloseableEntity centity = (CloseableEntity) entity;
			centity.setEntityDelegate(gunzip(centity.getEntityDelegate()));
			return centity;
		}
		return new GUnzipEntity(entity);
	}
}
