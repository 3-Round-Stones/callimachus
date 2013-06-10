/*
 * Copyright 2013, 3 Round Stones Inc., Some rights reserved.
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.CompletedResponse;
import org.callimachusproject.server.helpers.ResourceTransaction;
import org.callimachusproject.server.helpers.ResponseBuilder;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts MethodNotAllowed, NotAcceptable, and BadRequest into HTTP responses.
 * 
 * @author James Leigh
 * 
 */
public class ResponseExceptionHandler implements AsyncExecChain {
	private final Logger logger = LoggerFactory
			.getLogger(ResponseExceptionHandler.class);
	private final AsyncExecChain delegate;

	public ResponseExceptionHandler(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<CloseableHttpResponse> execute(HttpRoute route,
			final HttpRequestWrapper request, final HttpContext context,
			HttpExecutionAware execAware,
			FutureCallback<CloseableHttpResponse> callback) throws IOException,
			HttpException {
		callback = new ResponseCallback(callback) {
			public void failed(Exception ex) {
				try {
					throw ex;
				} catch (ResponseException e) {
					super.completed(handle(request, context, e));
				} catch (Exception e) {
					String uri = request.getRequestLine().getUri();
					logger.error("Internal Server Error while responding to "
							+ uri, e);
					super.completed(handle(request, context, new InternalServerError(e)));
				}
			}
		};
		try {
			return delegate.execute(route, request, context, execAware,
					callback);
		} catch (Exception ex) {
			return new CompletedResponse(callback, ex);
		}
	}

	HttpUriResponse handle(final HttpRequestWrapper request,
			final HttpContext context, ResponseException e) {
		ResourceTransaction trans = CalliContext.adapt(context).getResourceTransaction();
		HttpUriResponse resp = new ResponseBuilder(trans).exception(e);
		Set<String> allowed = trans.getAllowedMethods();
		if (allowed.isEmpty())
			return resp;
		StringBuilder sb = new StringBuilder();
		for (String method : allowed) {
			sb.append(method).append(",");
		}
		String allow = sb.substring(0, sb.length() - 1);
		resp.addHeader("Allow", allow);
		return resp;
	}

}
