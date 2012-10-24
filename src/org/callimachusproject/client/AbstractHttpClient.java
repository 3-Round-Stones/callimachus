/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.callimachusproject.client;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpClient implements HttpClient {
	private final Logger logger = LoggerFactory.getLogger(AbstractHttpClient.class);

	public HttpResponse execute(HttpUriRequest request) throws IOException,
			ClientProtocolException {
		return execute(request, (HttpContext) null);
	}

	public HttpResponse execute(HttpHost host, HttpRequest request)
			throws IOException, ClientProtocolException {
		return execute(host, request, (HttpContext) null);
	}

	public HttpResponse execute(HttpUriRequest request, HttpContext context)
			throws IOException, ClientProtocolException {
		if (request == null) {
			throw new IllegalArgumentException("Request must not be null.");
		}
		return execute(determineTarget(request), request, context);
	}

	public <T> T execute(HttpUriRequest request,
			ResponseHandler<? extends T> responseHandler) throws IOException,
			ClientProtocolException {
		return execute(request, responseHandler, null);
	}

	public <T> T execute(HttpHost host, HttpRequest request,
			ResponseHandler<? extends T> responseHandler) throws IOException,
			ClientProtocolException {
		return execute(host, request, responseHandler, null);
	}

	public <T> T execute(HttpUriRequest request,
			ResponseHandler<? extends T> responseHandler, HttpContext context)
			throws IOException, ClientProtocolException {
		HttpHost target = determineTarget(request);
		return execute(target, request, responseHandler, context);
	}

	public <T> T execute(HttpHost host, HttpRequest request,
			ResponseHandler<? extends T> responseHandler, HttpContext context)
			throws IOException, ClientProtocolException {
        if (responseHandler == null) {
            throw new IllegalArgumentException
                ("Response handler must not be null.");
        }

        HttpResponse response = execute(host, request, context);

        T result;
        try {
            result = responseHandler.handleResponse(response);
        } catch (Exception t) {
            HttpEntity entity = response.getEntity();
            try {
                EntityUtils.consume(entity);
            } catch (Exception t2) {
                // Log this exception. The original exception is more
                // important and will be thrown to the caller.
                logger.warn("Error consuming content after an exception.", t2);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            throw new UndeclaredThrowableException(t);
        }

        // Handling the response was successful. Ensure that the content has
        // been fully consumed.
        HttpEntity entity = response.getEntity();
        EntityUtils.consume(entity);
        return result;
	}

	private static HttpHost determineTarget(HttpUriRequest request)
			throws ClientProtocolException {
		// A null target may be acceptable if there is a default target.
		// Otherwise, the null target is detected in the director.
		HttpHost target = null;

		URI requestURI = request.getURI();
		if (requestURI.isAbsolute()) {
			target = URIUtils.extractHost(requestURI);
			if (target == null) {
				throw new ClientProtocolException(
						"URI does not specify a valid host name: " + requestURI);
			}
		}
		return target;
	}

}
