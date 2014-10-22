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

import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.annotations.query;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.server.exceptions.MethodNotAllowed;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.ResourceOperation;
import org.callimachusproject.server.helpers.ResponseBuilder;

/**
 * If a GET request cannot be satisfied send a redirect to another operation.
 * 
 * @author James Leigh
 * 
 */
public class AlternativeHandler implements ClientExecChain {
	private final ClientExecChain delegate;

	public AlternativeHandler(ClientExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public CloseableHttpResponse execute(HttpRoute route,
			HttpRequestWrapper request, HttpClientContext context,
			HttpExecutionAware execAware) throws IOException, HttpException {
		try {
			return delegate.execute(route, request, context, execAware);
		} catch (MethodNotAllowed e) {
			HttpUriResponse rb = findAlternate(request, context);
			if (rb != null)
				return rb;
			throw e;
		} catch (NotAcceptable e) {
			HttpUriResponse rb = findAlternate(request, context);
			if (rb != null)
				return rb;
			throw e;
		}
	}

	private HttpUriResponse findAlternate(HttpRequest request, HttpContext context) {
		ResourceOperation req = CalliContext.adapt(context).getResourceTransaction();
		String m = req.getMethod();
		if (!"GET".equals(m) && !"HEAD".equals(m))
			return null;
		Method operation;
		if (req.getOperation() != null && req.findMethodHandlers().isEmpty()) {
			return new ResponseBuilder(request, context).notFound(req.getRequestURL());
		} else if (req.getOperation() != null) {
			return null;
		} else if ((operation = req.getAlternativeMethod("alternate")) != null) {
			String loc = req.getRequestURI() + "?" + getQuery(operation);
			return new ResponseBuilder(request, context).found(loc);
		} else if ((operation = req.getAlternativeMethod("describedby")) != null) {
			String loc = req.getRequestURI() + "?" + getQuery(operation);
			return new ResponseBuilder(request, context).see(loc);
		} else {
			return new ResponseBuilder(request, context).notFound(req.getRequestURL());
		}
	}

	private String getQuery(Method operation) {
		return operation.getAnnotation(query.class).value()[0];
	}

}
