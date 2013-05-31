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
package org.callimachusproject.server.handlers;

import java.lang.reflect.Method;

import org.apache.http.protocol.HttpContext;
import org.callimachusproject.annotations.query;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.MethodNotAllowed;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.ResponseBuilder;
import org.callimachusproject.server.model.ResourceOperation;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

/**
 * If a GET request cannot be satisfied send a redirect to another operation.
 * 
 * @author James Leigh
 * 
 */
public class AlternativeHandler implements Handler {
	private final Handler delegate;

	public AlternativeHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public HttpUriResponse verify(ResourceOperation req, HttpContext context) throws Exception {
		try {
			return delegate.verify(req, context);
		} catch (MethodNotAllowed e) {
			HttpUriResponse rb = findAlternate(req);
			if (rb != null)
				return rb;
			throw e;
		} catch (NotAcceptable e) {
			HttpUriResponse rb = findAlternate(req);
			if (rb != null)
				return rb;
			throw e;
		} catch (BadRequest e) {
			HttpUriResponse rb = findAlternate(req);
			if (rb != null)
				return rb;
			throw e;
		}
	}

	public HttpUriResponse handle(ResourceOperation req, HttpContext context) throws Exception {
		try {
			return delegate.handle(req, context);
		} catch (MethodNotAllowed e) {
			HttpUriResponse rb = findAlternate(req);
			if (rb != null)
				return rb;
			throw e;
		} catch (NotAcceptable e) {
			HttpUriResponse rb = findAlternate(req);
			if (rb != null)
				return rb;
			throw e;
		}
	}

	private HttpUriResponse findAlternate(ResourceOperation req)
			throws RepositoryException,
			QueryEvaluationException {
		String m = req.getMethod();
		if (req.getOperation() != null
				|| !("GET".equals(m) || "HEAD".equals(m)))
			return null;
		Method operation;
		if ((operation = req.getAlternativeMethod("alternate")) != null) {
			String loc = req.getRequestURI() + "?" + getQuery(operation);
			return new ResponseBuilder(req).found(loc);
		} else if ((operation = req.getAlternativeMethod("describedby")) != null) {
			String loc = req.getRequestURI() + "?" + getQuery(operation);
			return new ResponseBuilder(req).see(loc);
		} else if (req.getOperation() == null && ("GET".equals(m) || "HEAD".equals(m))) {
			return new ResponseBuilder(req).notFound();
		} else if (req.findMethodHandlers().isEmpty() && ("GET".equals(m) || "HEAD".equals(m))) {
			return new ResponseBuilder(req).notFound();
		}
		return null;
	}

	private String getQuery(Method operation) {
		return operation.getAnnotation(query.class).value()[0];
	}

}
