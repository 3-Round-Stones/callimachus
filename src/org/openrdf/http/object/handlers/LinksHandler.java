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
package org.openrdf.http.object.handlers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.activation.MimeTypeParseException;

import org.openrdf.http.object.annotations.rel;
import org.openrdf.http.object.annotations.title;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

/**
 * Add an HTTP Header called 'Link' with other operations available to this
 * resource.
 * 
 * @author James Leigh
 * 
 */
public class LinksHandler implements Handler {
	private final Handler delegate;
	private String envelopeType;

	public LinksHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public String getEnvelopeType() {
		return envelopeType;
	}

	public void setEnvelopeType(String envelopeType) {
		this.envelopeType = envelopeType;
	}

	public Response verify(ResourceOperation request) throws Exception {
		return delegate.verify(request);
	}

	public Response handle(ResourceOperation req) throws Exception {
		Response rb = delegate.handle(req);
		String method = req.getMethod();
		int status = rb.getStatus();
		if (("GET".equals(method) || "HEAD".equals(method)) && 200 <= status
				&& status < 400) {
			return addLinks(req, rb);
		} else if ("OPTIONS".equals(method)) {
			return addLinks(req, rb);
		} else {
			return rb;
		}
	}

	private Response addLinks(ResourceOperation request, Response rb)
			throws RepositoryException, QueryEvaluationException,
			MimeTypeParseException {
		if (!request.isQueryStringPresent()) {
			for (String link : getLinks(request)) {
				rb = rb.header("Link", link);
			}
		}
		return rb;
	}

	private List<String> getLinks(ResourceOperation request)
			throws RepositoryException, QueryEvaluationException {
		Map<String, List<Method>> map = request
				.getOperationMethods("GET", true);
		List<String> result = new ArrayList<String>(map.size());
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			sb.delete(0, sb.length());
			for (Method m : e.getValue()) {
				if (!m.isAnnotationPresent(rel.class))
					continue;
				if (sb.length() == 0) {
					sb.append("<").append(request.getRequestURI());
					sb.append("?").append(e.getKey()).append(">");
				}
				sb.append("; rel=\"");
				for (String value : m.getAnnotation(rel.class).value()) {
					sb.append(value).append(" ");
				}
				sb.setCharAt(sb.length() - 1, '"');
				if (m.isAnnotationPresent(type.class)) {
					boolean envolope = false;
					String[] values = m.getAnnotation(type.class).value();
					if (envelopeType != null) {
						for (String value : values) {
							if (value.startsWith(envelopeType)) {
								envolope = true;
							}
						}
					}
					if (!envolope) {
						sb.append("; type=\"");
						for (String value : values) {
							sb.append(value).append(" ");
						}
						sb.setCharAt(sb.length() - 1, '"');
					}
				}
				if (m.isAnnotationPresent(title.class)) {
					for (String value : m.getAnnotation(title.class).value()) {
						sb.append("; title=\"").append(value).append("\"");
					}
				}
			}
			if (sb.length() > 0) {
				result.add(sb.toString());
			}
		}
		return result;
	}

}
