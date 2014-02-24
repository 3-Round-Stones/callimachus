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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.message.BasicHttpResponse;

public class UnavailableRequestDirector implements ClientExecChain {

	@Override
	public CloseableHttpResponse execute(HttpRoute route,
			HttpRequestWrapper request, HttpClientContext clientContext,
			HttpExecutionAware execAware) throws IOException, HttpException {
		BasicHttpResponse _503 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 503, "Service Disconnected");
		HttpHost target = route.getTargetHost();
		try {
			URI root = new URI(target.getSchemeName(), null, target.getHostName(), target.getPort(), "/", null, null);
			return new HttpUriResponse(root.resolve(request.getURI()).toASCIIString(), _503);
		} catch (URISyntaxException e) {
			return new HttpUriResponse(request.getURI().toASCIIString(), _503);
		}
	}

}
