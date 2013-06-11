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

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.ResourceTransaction;
import org.callimachusproject.server.helpers.ResponseBuilder;

/**
 * Responds with 412 if the resource has been modified.
 * 
 * @author James Leigh
 * 
 */
public class UnmodifiedSinceHandler implements AsyncExecChain {
	private final AsyncExecChain delegate;

	public UnmodifiedSinceHandler(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target,
			HttpRequest request, HttpContext context,
			FutureCallback<HttpResponse> callback) {
		ResourceTransaction trans = CalliContext.adapt(context).getResourceTransaction();
		String contentType = trans.getResponseContentType();
		String cache = trans.getResponseCacheControl();
		String entityTag = trans.getEntityTag(request, trans.getContentVersion(), cache, contentType);
		if (unmodifiedSince(trans, entityTag)) {
			return delegate.execute(target, request, context, callback);
		} else {
			BasicFuture<HttpResponse> future;
			future = new BasicFuture<HttpResponse>(callback);
			future.completed(new ResponseBuilder(trans).preconditionFailed("Resource has since been modified"));
			return future;
		}
	}

	private boolean unmodifiedSince(ResourceTransaction request, String entityTag) {
		long lastModified = request.getLastModified();
		Enumeration matchs = request.getHeaderEnumeration("If-Match");
		boolean mustMatch = matchs.hasMoreElements();
		try {
			if (lastModified > 0) {
				long unmodified = request.getDateHeader("If-Unmodified-Since");
				if (unmodified > 0 && lastModified > unmodified)
					return false;
			}
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		while (matchs.hasMoreElements()) {
			String match = (String) matchs.nextElement();
			if (match(entityTag, match, request.isSafe()))
				return true;
		}
		return !mustMatch;
	}

	private boolean match(String tag, String match, boolean safe) {
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
		if (td >= 0 && md >= 0 && safe)
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
