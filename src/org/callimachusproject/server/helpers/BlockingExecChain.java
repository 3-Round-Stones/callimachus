/*
 * Copyright 2013, 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.server.helpers;

import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.AsyncExecChain;

public class BlockingExecChain implements AsyncExecChain {
	private final ClientExecChain delegate;

	public BlockingExecChain(ClientExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<CloseableHttpResponse> execute(final HttpRoute route,
			final HttpRequestWrapper request, final HttpContext context,
			final HttpExecutionAware execAware,
			final FutureCallback<CloseableHttpResponse> callback) {
		final BasicFuture<CloseableHttpResponse> future;
		future = new BasicFuture<CloseableHttpResponse>(callback);
		try {
			CloseableHttpResponse ret;
			ret = delegate.execute(route, request, HttpClientContext.adapt(context), execAware);
			future.completed(ret);
		} catch (HttpException ex) {
			future.failed(ex);
		} catch (IOException ex) {
			future.failed(ex);
		} catch (RuntimeException ex) {
			future.failed(ex);
		} finally {
			if (!future.isDone()) {
				future.cancel();
			}
		}
		return future;
	}

}
