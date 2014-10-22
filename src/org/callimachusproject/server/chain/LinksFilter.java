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
package org.callimachusproject.server.chain;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.annotations.rel;
import org.callimachusproject.annotations.title;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.ResourceOperation;
import org.callimachusproject.server.helpers.ResponseCallback;

/**
 * Add an HTTP Header called 'Link' with other operations available to this
 * resource.
 * 
 * @author James Leigh
 * 
 */
public class LinksFilter implements AsyncExecChain {
	private final AsyncExecChain delegate;
	private String envelopeType;

	public LinksFilter(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	public String getEnvelopeType() {
		return envelopeType;
	}

	public void setEnvelopeType(String envelopeType) {
		this.envelopeType = envelopeType;
	}

	public Future<HttpResponse> execute(HttpHost target,
			HttpRequest request, HttpContext context,
			FutureCallback<HttpResponse> callback) {
		String m = request.getRequestLine().getMethod();
		if ("GET".equals(m) || "HEAD".equals(m) || "OPTIONS".equals(m)) {
			final ResourceOperation req = CalliContext.adapt(context).getResourceTransaction();
			return delegate.execute(target, request, context, new ResponseCallback(callback) {
				public void completed(HttpResponse result) {
					try {
						addLinks(req, result);
						super.completed(result);
					} catch (RuntimeException ex) {
						super.failed(ex);
					}
				}
			});
		} else {
			return delegate.execute(target, request, context, callback);
		}
	}

	void addLinks(ResourceOperation request, HttpResponse rb) {
		String name = request.getOperation();
		if (name == null) {
			for (String link : getRelLinks(request)) {
				rb.addHeader("Link", link);
			}
		} else {
			for (String link : getRevLinks(request, name)) {
				rb.addHeader("Link", link);
			}
		}
	}

	private List<String> getRelLinks(ResourceOperation request) {
		Map<String, List<Method>> map = request
				.getOperationMethods("GET", true);
		List<String> result = new ArrayList<String>(map.size());
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			String query = e.getKey();
			List<Method> methods = e.getValue();
			getLinks(request, query, false, methods, result);
		}
		return result;
	}

	private List<String> getRevLinks(ResourceOperation request, String name) {
		List<Method> list = request.getOperationMethods("GET", true).get(name);
		if (list == null)
			return Collections.emptyList();
		List<String> result = new ArrayList<String>(list.size());
		getLinks(request, null, true, list, result);
		return result;
	}

	private void getLinks(ResourceOperation request, String query, boolean rev,
			List<Method> methods, List<String> result) {
		String uri = request.getRequestURI();
		Set<String> rels = new LinkedHashSet<String>();
		Set<String> types = new LinkedHashSet<String>();
		Set<String> titles = new LinkedHashSet<String>();
		for (Method m : methods) {
			Collection<String> mrel = getMethodRel(m);
			if (!mrel.isEmpty()) {
				rels.addAll(mrel);
				types.addAll(getMethodResponseTypes(m, request));
				titles.addAll(getMethodTitles(m));
			}
		}
		if (!rels.isEmpty()) {
			result.add(serialize(uri, query, rev, rels, types, titles));
		}
	}

	private String serialize(String uri, String query, boolean rev,
			Set<String> rels, Set<String> types, Set<String> titles) {
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(uri);
		if (query != null && query.length() > 0) {
			sb.append("?").append(query);
		}
		sb.append(">");
		if (rev) {
			sb.append("; rev=\"");
		} else {
			sb.append("; rel=\"");
		}
		for (String rel : rels) {
			sb.append(rel).append(" ");
		}
		sb.setCharAt(sb.length() - 1, '"');
		if (!types.isEmpty()) {
			sb.append("; type=\"");
			for (String type : types) {
				sb.append(type).append(" ");
			}
			sb.setCharAt(sb.length() - 1, '"');
		}
		for (String title : titles) {
			sb.append("; title=\"").append(title).append("\"");
		}
		return sb.toString();
	}

	private Collection<String> getMethodRel(Method method) {
		if (method.isAnnotationPresent(rel.class))
			return Arrays.asList(method.getAnnotation(rel.class).value());
		return Collections.emptyList();
	}

	private Collection<String> getMethodTitles(Method method) {
		if (method.isAnnotationPresent(title.class))
			return Arrays.asList(method.getAnnotation(title.class).value());
		return Collections.emptyList();
	}

	private Collection<String> getMethodResponseTypes(Method method,
			ResourceOperation request) {
		Collection<String> values = getResponseType(request, method);
		if (values != null && !values.isEmpty()) {
			if (envelopeType != null) {
				Iterator<String> iter = values.iterator();
				while (iter.hasNext()) {
					if (iter.next().startsWith(envelopeType)) {
						iter.remove();
					}
				}
			}
		}
		return values;
	}

	private Collection<String> getResponseType(ResourceOperation request,
			Method m) {
		Collection<String> set = new LinkedHashSet<String>();
		type ann = m.getAnnotation(type.class);
		if (ann != null) {
			set.addAll(Arrays.asList(ann.value()));
		}
		return set;
	}

}
