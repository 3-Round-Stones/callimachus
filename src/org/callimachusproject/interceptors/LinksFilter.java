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
package org.callimachusproject.interceptors;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.annotations.rel;
import org.callimachusproject.annotations.title;
import org.openrdf.annotations.Path;
import org.openrdf.annotations.Type;
import org.openrdf.http.object.chain.HttpRequestChainInterceptor;
import org.openrdf.http.object.helpers.ObjectContext;
import org.openrdf.http.object.helpers.ResourceTarget;

/**
 * Add an HTTP Header called 'Link' with other operations available to this
 * resource.
 * 
 * @author James Leigh
 * 
 */
public class LinksFilter implements HttpRequestChainInterceptor {
	private final Pattern SIMPLE = Pattern
			.compile("^\\??([^\\.\\$\\|\\(\\)\\[\\{\\^\\?\\*\\+\\\\]|\\\\[^a-zA-Z0-9])*$");

	@Override
	public HttpResponse intercept(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return null;
	}

	@Override
	public void process(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		ObjectContext ctx = ObjectContext.adapt(context);
		String uri = request.getRequestLine().getUri();
		ResourceTarget resource = ctx.getResourceTarget();
		String iri = resource.getTargetObject().getResource().stringValue();
		if (iri.endsWith(uri)) {
			// no extra path or query string
			for (String link : getRelLinks(request, resource)) {
				response.addHeader("Link", link);
			}
		} else {
			Method handler = getHandlerMethod(request, resource);
			if (handler != null) {
				for (String link : getRevLinks(resource, handler)) {
					response.addHeader("Link", link);
				}
			}
		}
	}

	private List<String> getRelLinks(HttpRequest request, ResourceTarget resource) {
		Map<String, List<Method>> map = new LinkedHashMap<String, List<Method>>();
		for (java.lang.reflect.Method method : resource.getTargetObject().getClass().getMethods()) {
			org.openrdf.annotations.Method m = method.getAnnotation(org.openrdf.annotations.Method.class);
			if (m == null || !"GET".equals(m.value()))
				continue;
			Path p = method.getAnnotation(Path.class);
			if (p == null) {
				putAdd(map, "", method);
			} else {
				for (String regex : p.value()) {
					if (SIMPLE.matcher(regex).matches()) {
						putAdd(map, regex.replace("\\", ""), method);
					}
				}
			}
		}
		String iri = resource.getTargetObject().getResource().stringValue();
		List<String> result = new ArrayList<String>();
		for (Map.Entry<String, List<Method>> e : map.entrySet()) {
			String suffix = e.getKey();
			List<Method> methods = e.getValue();
			getLinks(iri, suffix, false, methods, result);
		}
		return result;
	}

	private <T> void putAdd(Map<String, List<T>> map, String suffix, T method) {
		List<T> list = map.get(suffix);
		if (list == null) {
			map.put(suffix, list = new ArrayList<T>());
		}
		list.add(method);
	}

	private List<String> getRevLinks(ResourceTarget resource, Method handler) {
		Path p = handler.getAnnotation(Path.class);
		if (p == null)
			return Collections.emptyList();
		List<String> paths = Arrays.asList(p.value());
		List<Method> list = new ArrayList<Method>();
		for (java.lang.reflect.Method method : resource.getTargetObject().getClass().getMethods()) {
			org.openrdf.annotations.Method m = method.getAnnotation(org.openrdf.annotations.Method.class);
			if (m == null || !"GET".equals(m.value()))
				continue;
			Path path = method.getAnnotation(Path.class);
			if (path != null && Arrays.asList(path.value()).containsAll(paths)) {
				list.add(method);
			}
		}
		if (list.isEmpty())
			return Collections.emptyList();
		String iri = resource.getTargetObject().getResource().stringValue();
		List<String> result = new ArrayList<String>(list.size());
		getLinks(iri, null, true, list, result);
		return result;
	}

	private Method getHandlerMethod(HttpRequest request, ResourceTarget target) {
		Method method = target.getHandlerMethod(request);
		RequestLine line = request.getRequestLine();
		if (method != null || !"HEAD".equals(line.getMethod()))
			return method;
		HttpRequest get = new BasicHttpRequest("GET", line.getUri(), line.getProtocolVersion());
		get.setHeaders(request.getAllHeaders());
		return target.getHandlerMethod(get);
	}

	private void getLinks(String uri, String suffix, boolean rev,
			List<Method> methods, List<String> result) {
		Set<String> rels = new LinkedHashSet<String>();
		Set<String> types = new LinkedHashSet<String>();
		Set<String> titles = new LinkedHashSet<String>();
		for (Method m : methods) {
			Collection<String> mrel = getMethodRel(m);
			if (!mrel.isEmpty()) {
				rels.addAll(mrel);
				for (String type : getResponseType(m)) {
					try {
						MimeType mtype = new MimeType(type);
						mtype.removeParameter("q");
						types.add(mtype.toString());
					} catch (MimeTypeParseException e) {
						types.add(type);
					}
				}
				titles.addAll(getMethodTitles(m));
			}
		}
		if (!rels.isEmpty()) {
			result.add(serialize(uri, suffix, rev, rels, types, titles));
		}
	}

	private String serialize(String uri, String suffix, boolean rev,
			Set<String> rels, Set<String> types, Set<String> titles) {
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(uri);
		if (suffix != null && suffix.length() > 0) {
			sb.append(suffix);
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

	private Collection<String> getResponseType(Method m) {
		Collection<String> set = new LinkedHashSet<String>();
		Type ann = m.getAnnotation(Type.class);
		if (ann != null) {
			set.addAll(Arrays.asList(ann.value()));
		}
		return set;
	}

}
