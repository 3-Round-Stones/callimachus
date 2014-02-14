/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.server.chain;

import java.nio.charset.Charset;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.CompletedResponse;

public class SecureChannelFilter implements AsyncExecChain {
	private final AsyncExecChain delegate;

	public SecureChannelFilter(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target, HttpRequest request,
			HttpContext context, FutureCallback<HttpResponse> callback) {
		String scheme = target.getSchemeName();
		String protocol = CalliContext.adapt(context).getProtocolScheme();
		if ("https".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(protocol))
			return new CompletedResponse(callback, insecure());
		return delegate.execute(target, request, context, callback);
	}

	private BasicHttpResponse insecure() {
		String msg = "Cannot request secure resource over insecure channel";
		BasicHttpResponse resp;
		resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 400, msg);
		resp.setEntity(new StringEntity(msg, Charset.forName("UTF-8")));
		return resp;
	}

}
