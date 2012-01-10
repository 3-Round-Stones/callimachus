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
package org.openrdf.http.object.handlers;

import javax.activation.MimeTypeParseException;

import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.http.object.util.HTTPDateFormat;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

/**
 * Adds the HTTP headers: Cache-Control, Vary, ETag, Content-Type,
 * Content-Encoding, and Last-Modified.
 * 
 * @author James Leigh
 * 
 */
public class ContentHeadersHandler implements Handler {
	private final Handler delegate;
	private final HTTPDateFormat format = new HTTPDateFormat();

	public ContentHeadersHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public Response verify(ResourceOperation request) throws Exception {
		Response rb = delegate.verify(request);
		if (rb != null) {
			Class<?> type = request.getEntityType();
			String contentType = request.getResponseContentType();
			String encoding = request.getResponseContentEncoding();
			String version = request.revision();
			String cache = request.getResponseCacheControl();
			addHeaders(request, type, contentType, encoding, version, cache, rb);
		}
		return rb;
	}

	public Response handle(ResourceOperation request) throws Exception {
		Class<?> type = request.getEntityType();
		String contentType = request.getResponseContentType();
		String encoding = request.getResponseContentEncoding();
		String derived = request.revision();
		String cache = request.getResponseCacheControl();
		Response rb = delegate.handle(request);
		addHeaders(request, type, contentType, encoding, derived, cache, rb);
		return rb;
	}

	private void addHeaders(ResourceOperation request, Class<?> type,
			String contentType, String contentEncoding, String derived,
			String cache, Response rb) throws MimeTypeParseException,
			RepositoryException, QueryEvaluationException {
		String version = request.isSafe() ? derived : request.revision();
		String entityTag = request.getEntityTag(version, cache, contentType);
		long lastModified = request.getLastModified();
		if (cache != null) {
			rb.setHeader("Cache-Control", cache);
		}
		for (String vary : request.getVary()) {
			if (!vary.equalsIgnoreCase("Authorization")) {
				rb.header("Vary", vary);
			}
		}
		if (version != null && !rb.containsHeader("Content-Version")) {
			rb.setHeader("Content-Version", "\"" + version + "\"");
		}
		if (derived != null && !derived.equals(version)
				&& !rb.containsHeader("Derived-From")) {
			rb.setHeader("Derived-From", "\"" + derived + "\"");
		}
		if (entityTag != null && !rb.containsHeader("ETag")) {
			rb.setHeader("ETag", entityTag);
		}
		if (contentType != null && rb.isContent()) {
			rb.setHeader("Content-Type", contentType);
		}
		if (contentEncoding != null && rb.isContent()) {
			rb.setHeader("Content-Encoding", contentEncoding);
		}
		if (lastModified > 0) {
			rb.lastModified(lastModified, format.format(lastModified));
		}
		rb.setEntityType(type);
	}

}
