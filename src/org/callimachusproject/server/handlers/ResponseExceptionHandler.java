/*
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
package org.callimachusproject.server.handlers;

import java.util.Set;

import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.MethodNotAllowed;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.ResponseBuilder;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

/**
 * Converts MethodNotAllowed, NotAcceptable, and BadRequest into HTTP responses.
 * 
 * @author James Leigh
 * 
 */
public class ResponseExceptionHandler implements Handler {
	private final Handler delegate;

	public ResponseExceptionHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public HttpUriResponse verify(ResourceOperation request, HttpContext context) throws Exception {
		try {
			return delegate.verify(request, context);
		} catch (MethodNotAllowed e) {
			return methodNotAllowed(request, new ResponseBuilder(request).exception(e));
		} catch (NotAcceptable e) {
			return new ResponseBuilder(request).exception(e);
		} catch (BadRequest e) {
			return new ResponseBuilder(request).exception(e);
		}
	}

	public HttpUriResponse handle(ResourceOperation request, HttpContext context) throws Exception {
		try {
			return delegate.handle(request, context);
		} catch (MethodNotAllowed e) {
			return methodNotAllowed(request, new ResponseBuilder(request).exception(e));
		} catch (NotAcceptable e) {
			return new ResponseBuilder(request).exception(e);
		}
	}

	private HttpUriResponse methodNotAllowed(ResourceOperation request, HttpUriResponse resp)
			throws RepositoryException, QueryEvaluationException {
		Set<String> allowed = request.getAllowedMethods();
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
