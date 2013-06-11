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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.AsyncExecChain;

public class PooledExecChain implements AsyncExecChain {
	final AsyncExecChain delegate;
	private final ExecutorService executor;

	public PooledExecChain(ClientExecChain delegate, ExecutorService executor) {
		this(new BlockingExecChain(delegate), executor);
	}

	public PooledExecChain(AsyncExecChain delegate, ExecutorService executor) {
		this.delegate = delegate;
		this.executor = executor;
	}

	@Override
	public Future<HttpResponse> execute(final HttpHost target,
			final HttpRequest request, final HttpContext context,
			final FutureCallback<HttpResponse> callback) {
		try {
			final DelegatingFuture future = new DelegatingFuture(callback);
			final Future<?> first = executor.submit(new Runnable() {
				public void run() {
					try {
						if (future.isCancelled()) {
							future.cancelled();
						} else {
							future.setDelegate(delegate.execute(target, request,
									context, future));
						}
					} catch (RuntimeException ex) {
						future.failed(ex);
					} catch (Error ex) {
						future.cancelled();
						throw ex;
					}
				}
			});
			future.setDelegateIfNull(first);
			return future;
		} catch (RejectedExecutionException e) {
			CompletedResponse future = new CompletedResponse(callback);
			future.cancel();
			return future;
		}
	}

}
