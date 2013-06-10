/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.Request;

/**
 * If the request has Content-MD5 header, ensure the request body matches.
 */
public class MD5ValidationFilter implements AsyncExecChain {
	private final AsyncExecChain delegate;

	public MD5ValidationFilter(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<CloseableHttpResponse> execute(HttpRoute route,
			HttpRequestWrapper request, HttpContext context,
			HttpExecutionAware execAware,
			FutureCallback<CloseableHttpResponse> callback) throws IOException,
			HttpException {
		Request req = new Request(request);
		if (req.containsHeader("Content-MD5") && req.getEntity() != null) {
			String md5 = req.getHeader("Content-MD5");
			req.setEntity(new MD5ValidationEntity(req.getEntity(), md5));
			return delegate.execute(route, HttpRequestWrapper.wrap(req), context, execAware, callback);
		} else {
			return delegate.execute(route, request, context, execAware, callback);
		}
	}

}
