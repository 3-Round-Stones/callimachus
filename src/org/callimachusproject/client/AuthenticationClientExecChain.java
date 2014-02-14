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
package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.util.EntityUtils;

public class AuthenticationClientExecChain implements ClientExecChain {
	private final HttpAuthenticator authenticator;
	private final ClientExecChain delegate;

	public AuthenticationClientExecChain(ClientExecChain delegate) {
		this.authenticator = new HttpAuthenticator();
		this.delegate = delegate;
	}

	public CloseableHttpResponse execute(final HttpRoute route,
			final HttpRequestWrapper original, final HttpClientContext context,
			final HttpExecutionAware execAware) throws IOException,
			HttpException {
		final HttpRequestWrapper request = HttpRequestWrapper.wrap(original);
		final RequestConfig config = context.getRequestConfig();

		CloseableHttpResponse response = null;
		while (true) {
			if (execAware != null && execAware.isAborted()) {
				throw new RequestAbortedException("Request aborted");
			}

			this.authenticator.generateAuthResponse(route, request, context);

			response = delegate.execute(route, request, context, execAware);

			if (config.isAuthenticationEnabled()
					&& authenticator.needAuthentication(route, request,
							response, context)) {
				EntityUtils.consume(response.getEntity());
			} else {
				return response;
			}
		}
	}

}
