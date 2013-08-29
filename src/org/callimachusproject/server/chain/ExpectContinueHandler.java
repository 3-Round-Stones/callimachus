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
package org.callimachusproject.server.chain;

import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.Exchange;

public class ExpectContinueHandler implements AsyncExecChain {

	private final AsyncExecChain delegate;

	public ExpectContinueHandler(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target, HttpRequest request,
			HttpContext context, FutureCallback<HttpResponse> callback) {
		Exchange exchange = CalliContext.adapt(context).getExchange();
		if (exchange != null) {
			Header expect = request.getFirstHeader("Expect");
			if (expect != null
					&& expect.getValue().equalsIgnoreCase("100-continue")) {
				BasicHttpResponse _100 = new BasicHttpResponse(
						HttpVersion.HTTP_1_1, 100, "Continue");
				exchange.submitContinue(_100);
			}
		}
		return delegate.execute(target, request, context, callback);
	}

}
