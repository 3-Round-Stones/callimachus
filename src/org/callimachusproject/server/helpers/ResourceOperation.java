/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.helpers;

import static java.lang.Integer.toHexString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.FileObject;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.callimachusproject.annotations.expect;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.rel;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.MethodNotAllowed;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.exceptions.UnsupportedMediaType;
import org.callimachusproject.traits.VersionedObject;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.ParameterTypes;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class ResourceOperation {
	private static final String SUB_CLASS_OF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
	private final Logger logger = LoggerFactory.getLogger(ResourceOperation.class);

	private final FluidFactory ff = FluidFactory.getInstance();
	private final Request request;
	private final ValueFactory vf;
	private final ObjectConnection con;
	private VersionedObject target;
	private final URI uri;
	private final FluidBuilder writer;
	private final FluidType accepter;
	private final Set<String> vary = new LinkedHashSet<String>();
	private Method method;
	private MethodNotAllowed notAllowed;
	private BadRequest badRequest;
	private NotAcceptable notAcceptable;
	private UnsupportedMediaType unsupportedMediaType;

	public ResourceOperation(Request request, ObjectConnection con)
			throws QueryEvaluationException, RepositoryException {
		this.request = request;
		List<String> headers = getVaryHeaders("Accept");
		if (headers.isEmpty()) {
			accepter = new FluidType(HttpEntity.class);
		} else {
			StringBuilder sb = new StringBuilder();
			for (String hd : headers) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(hd);
			}
			accepter = new FluidType(HttpEntity.class, sb.toString().split("\\s*,\\s*"));
		}
		this.con = con;
		this.vf = con.getValueFactory();
		this.writer = ff.builder(con);
		try {
			this.uri = vf.createURI(request.getIRI());
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e);
		}
		target = con.getObjects(VersionedObject.class, uri).singleResult();
		try {
			String m = request.getMethod();
			if ("GET".equals(m) || "HEAD".equals(m)) {
				method = findMethod("GET", true);
			} else if ("PUT".equals(m) || "DELETE".equals(m)) {
				method = findMethod(m, null);
			} else {
				method = findMethod();
			}
		} catch (MethodNotAllowed e) {
			notAllowed = e;
		} catch (BadRequest e) {
			badRequest = e;
		} catch (NotAcceptable e) {
			notAcceptable = e;
		} catch (UnsupportedMediaType e) {
			unsupportedMediaType = e;
		}
	}

	public String getResponseContentType() {
		if (method == null || method.getReturnType().equals(Void.TYPE))
			return null;
		return getContentType(method);
	}

	public String getEntityTag(HttpRequest request, String version, String cache, String contentType) {
		Method m = this.method;
		int headers = getHeaderCodeFor(m);
		boolean strong = cache != null && cache.contains("cache-range");
		String method = request.getRequestLine().getMethod();
		if (contentType != null) {
			return variantTag(version, strong, contentType, headers);
		} else if ("GET".equals(method) || "HEAD".equals(method)) {
			if (m != null)
				return revisionTag(version, strong, headers);
			Method operation;
			if ((operation = getAlternativeMethod("alternate")) != null) {
				String type = getContentType(operation);
				headers = getHeaderCodeFor(operation);
				return variantTag(version, strong, type, headers);
			} else if ((operation = getAlternativeMethod("describedby")) != null) {
				String type = getContentType(operation);
				headers = getHeaderCodeFor(operation);
				return variantTag(version,strong,  type, headers);
			}
		} else {
			Header putContentType = null;
			if ("PUT".equals(method)) {
				putContentType = request.getFirstHeader("Content-Type");
			}
			Method get;
			try {
				headers = 0;
				get = findMethodIfPresent("GET", false, true);
				if (get != null) {
					headers = getHeaderCodeFor(get);
				}
			} catch (MethodNotAllowed e) {
				get = null;
			} catch (BadRequest e) {
				get = null;
			} catch (NotAcceptable e) {
				get = null;
			} catch (UnsupportedMediaType e) {
				get = null;
			}
			if (get == null && putContentType == null) {
				return revisionTag(version, strong, headers);
			} else if (get == null) {
				return variantTag(version, strong, putContentType.getValue(), headers);
			} else {
				String get_cache = getResponseCacheControlFor(get);
				boolean get_strong = get_cache != null && get_cache.contains("cache-range");
				return variantTag(version, get_strong, getContentType(get), headers);
			}
		}
		return null;
	}

	public String getResponseCacheControl() {
		return getResponseCacheControlFor(method);
	}

	private String getResponseCacheControlFor(Method m) {
		StringBuilder sb = new StringBuilder();
		if (m != null && m.isAnnotationPresent(header.class)) {
			for (String value : m.getAnnotation(header.class).value()) {
				int idx = value.indexOf(':');
				if (idx < 0)
					continue;
				String name = value.substring(0, idx);
				if (name.equalsIgnoreCase("cache-control")) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(value.substring(idx + 1));
				}
			}
		}
		setCacheControl(getRequestedResource().getClass(), sb);
		if (sb.indexOf("private") < 0 && sb.indexOf("public") < 0 && isPrivate(m)) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append("private");
		}
		if (sb.length() > 0)
			return sb.toString();
		return null;
	}

	public String[] getRequires() {
		if (method == null) {
			Set<String> list = new HashSet<String>();
			for (Method m : getRequestedResource().getClass().getMethods()) {
				requires ann = m.getAnnotation(requires.class);
				if (ann != null) {
					list.addAll(Arrays.asList(ann.value()));
				}
			}
			return list.toArray(new String[list.size()]);
		}
		requires ann = method.getAnnotation(requires.class);
		if (ann == null)
			return null;
		return ann.value();
	}

	public Set<String> getAllowedMethods() {
		Set<String> set = new LinkedHashSet<String>();
		String name = getOperation();
		RDFObject target = getRequestedResource();
		if (getOperationMethods("GET", true).containsKey(name)) {
			set.add("GET");
			set.add("HEAD");
		}
		if (getOperationMethods("PUT", false).containsKey(name)) {
			set.add("PUT");
		}
		if (getOperationMethods("DELETE", false).containsKey(name)) {
			set.add("DELETE");
		}
		Map<String, List<Method>> map = getPostMethods(target);
		for (String method : map.keySet()) {
			set.add(method);
			if ("GET".equals(method)) {
				set.add("HEAD");
			}
		}
		return set;
	}

	public Method getJavaMethod() {
		if (notAllowed != null)
			throw notAllowed;
		if (badRequest != null)
			throw badRequest;
		if (notAcceptable != null)
			throw notAcceptable;
		if (unsupportedMediaType != null)
			throw unsupportedMediaType;
		return method;
	}

	public Collection<Method> findMethodHandlers() {
		Collection<Method> methods = new ArrayList<Method>();
		RDFObject target = getRequestedResource();
		for (Method m : target.getClass().getMethods()) {
			if (m.isAnnotationPresent(ParameterTypes.class))
				continue;
			if (m.isAnnotationPresent(method.class)
					|| m.isAnnotationPresent(query.class)) {
				methods.add(m);
			}
		}
		return methods;
	}

	public Collection<Method> findMethodHandlers(String req_method) {
		if (req_method == null)
			return findMethodHandlers();
		Collection<Method> methods = new ArrayList<Method>();
		String name = getOperation();
		RDFObject target = getRequestedResource();
		if (name != null) {
			// lookup method
			List<Method> list = getOperationMethods(req_method, null).get(name);
			if (list != null) {
				methods.addAll(list);
			}
		}
		for (Method m : target.getClass().getMethods()) {
			if (m.isAnnotationPresent(ParameterTypes.class))
				continue;
			method ann = m.getAnnotation(method.class);
			if (ann == null)
				continue;
			if (!Arrays.asList(ann.value()).contains(req_method))
				continue;
			if (isOperationPresent(m))
				continue;
			if (name != null && isOperationProhibited(m))
				continue;
			methods.add(m);
		}
		return methods;
	}

	public Method getAlternativeMethod(String rel) {
		List<Method> methods = new ArrayList<Method>();
		for (Method m : getRequestedResource().getClass().getMethods()) {
			if (m.isAnnotationPresent(ParameterTypes.class))
				continue;
			if (m.getReturnType().equals(Void.TYPE))
				continue;
			if (!m.isAnnotationPresent(rel.class))
				continue;
			if (!m.isAnnotationPresent(query.class))
				continue;
			if (isRequestBody(m))
				continue;
			for (String value : m.getAnnotation(rel.class).value()) {
				if (!rel.equals(value))
					continue;
				if (m.isAnnotationPresent(method.class)) {
					for (String v : m.getAnnotation(method.class).value()) {
						if ("GET".equals(v)) {
							methods.add(m);
							break;
						}
					}
				} else {
					methods.add(m);
				}
			}
		}
		try {
			return findBestMethod(findAcceptableMethods(methods, false));
		} catch (NotAcceptable e) {
			return null;
		}
	}

	public Map<String, List<Method>> getOperationMethods(String method,
			Boolean isRespBody) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : getRequestedResource().getClass().getMethods()) {
			if (m.isAnnotationPresent(ParameterTypes.class))
				continue;
			boolean content = !m.getReturnType().equals(Void.TYPE);
			if (isRespBody != null && isRespBody != content)
				continue;
			String[] query = null;
			if (m.isAnnotationPresent(query.class)) {
				query = m.getAnnotation(query.class).value();
			}
			if (query == null || query.length == 0) {
				if (m.isAnnotationPresent(method.class)) {
					query = new String[]{null};
				} else {
					continue;
				}
			}
			if (m.isAnnotationPresent(method.class)) {
				for (String v : m.getAnnotation(method.class).value()) {
					if (method.equals(v)) {
						put(map, query, m);
						break;
					}
				}
			} else if ("OPTIONS".equals(method)) {
				put(map, query, m);
			} else {
				boolean body = isRequestBody(m);
				if (("GET".equals(method) || "HEAD".equals(method)) && content
						&& !body) {
					put(map, query, m);
				} else if (("PUT".equals(method) || "DELETE".equals(method))
						&& !content && body) {
					put(map, query, m);
				} else if ("POST".equals(method) && content && body) {
					put(map, query, m);
				}
			}
		}
		return map;
	}

	public String toString() {
		return request.toString();
	}

	public String getVaryHeader(String name) {
		if (!vary.contains(name)) {
			vary.add(name);
		}
		return request.getHeader(name);
	}

	public List<String> getVaryHeaders(String... name) {
		vary.addAll(Arrays.asList(name));
		List<String> values = new ArrayList<String>();
		for (Header hd : request.getAllHeaders()) {
			for (String name1 : name) {
				if (name1.equalsIgnoreCase(hd.getName())) {
					values.add(hd.getValue());
				}
			}
		}
		return values;
	}

	public Collection<String> getVary() {
		return vary;
	}

	public URI createURI(String uriSpec) {
		return vf.createURI(request.resolve(uriSpec));
	}

	public void flush() throws RepositoryException, QueryEvaluationException,
			IOException {
		this.target = con.getObject(VersionedObject.class, getRequestedResource().getResource());
	}

	public Fluid getBody() {
		String mediaType = request.getHeader("Content-Type");
		String location = request.getResolvedHeader("Content-Location");
		if (location == null) {
			location = request.getIRI();
		} else {
			location = createURI(location).stringValue();
		}
		FluidType ftype = new FluidType(HttpEntity.class, mediaType);
		return getFluidBuilder().consume(request.getEntity(), location, ftype);
	}

	public String getContentType(Method method) {
		Type genericType = method.getGenericReturnType();
		String[] mediaTypes = getTypes(method);
		return writer.nil(new FluidType(genericType, mediaTypes)).toMedia(accepter);
	}

	public String[] getTypes(Method method) {
		if (method.isAnnotationPresent(type.class))
			return method.getAnnotation(type.class).value();
		return new String[0];
	}

	public FluidBuilder getFluidBuilder() {
		return FluidFactory.getInstance().builder(con);
	}

	public String getOperation() {
		String qs = request.getQueryString();
		if (qs == null)
			return null;
		int a = qs.indexOf('&');
		int s = qs.indexOf(';');
		int e = qs.indexOf('=');
		try {
			if (a < 0 && s < 0 && e < 0)
				return URLDecoder.decode(qs, "UTF-8");
			if (a > 0 && (a < s || s < 0) && (a < e || e < 0))
				return URLDecoder.decode(qs.substring(0, a), "UTF-8");
			if (s > 0 && (s < e || e < 0))
				return URLDecoder.decode(qs.substring(0, s), "UTF-8");
			if (e > 0)
				return URLDecoder.decode(qs.substring(0, e), "UTF-8");
		} catch (UnsupportedEncodingException exc) {
			throw new AssertionError(exc);
		}
		return "";
	}

	public Fluid getHeader(String... names) {
		List<String> list = getVaryHeaders(names);
		String[] values = list.toArray(new String[list.size()]);
		FluidType ftype = new FluidType(String[].class, "text/plain", "text/*");
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		return fb.consume(values, request.getIRI(), ftype);
	}

	public Fluid getQueryStringParameter() {
		String value = request.getQueryString();
		FluidType ftype = new FluidType(String.class, "application/x-www-form-urlencoded", "text/*");
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		return fb.consume(value, request.getIRI(), ftype);
	}

	public RDFObject getRequestedResource() {
		return target;
	}

	public boolean isNoValidate() {
		String method = request.getMethod();
		Method m = this.method;
		if (m != null && !"PUT".equals(method) && !"DELETE".equals(method)
				&& !"OPTIONS".equals(method)) {
			if (m.isAnnotationPresent(header.class)) {
				for (String value : m.getAnnotation(header.class).value()) {
					int idx = value.indexOf(':');
					if (idx < 0)
						continue;
					String name = value.substring(0, idx);
					if (!name.equalsIgnoreCase("cache-control"))
						continue;
					if (value.contains("must-reevaluate"))
						return true;
					if (value.contains("no-validate"))
						return true;
				}
			}
		}
		RDFObject target = getRequestedResource();
		return noValidate(target.getClass());
	}

	public long getLastModified() {
		if (isNoValidate())
			return System.currentTimeMillis() / 1000 * 1000;
		if (target instanceof FileObject) {
			long lastModified = ((FileObject) target).getLastModified();
			if (lastModified > 0)
				return lastModified / 1000 * 1000;
		}
		try {
			Activity trans = target.getProvWasGeneratedBy();
			if (trans != null) {
				XMLGregorianCalendar xgc = trans.getProvEndedAtTime();
				if (xgc != null) {
					GregorianCalendar cal = xgc.toGregorianCalendar();
					cal.set(Calendar.MILLISECOND, 0);
					return cal.getTimeInMillis() / 1000 * 1000;
				}
			}
		} catch (ClassCastException e) {
			logger.warn(e.toString(), e);
		}
		return 0;
	}

	public String getContentVersion() {
		try {
			Activity activity = target.getProvWasGeneratedBy();
			if (activity == null)
				return null;
			String uri = ((RDFObject) activity).getResource().stringValue();
			int f = uri.indexOf('#');
			if (f >= 0) {
				uri = uri.substring(0, f);
			}
			String origin = request.getOrigin();
			if (uri.startsWith(origin) && '/' == uri.charAt(origin.length()))
				return uri.substring(origin.length());
			return uri;
		} catch (ClassCastException e) {
			return null;
		}
	}

	public FluidType getAcceptable() {
		return accepter;
	}

	public boolean isAcceptable(Type genericType, String... mediaType) {
		FluidType ftype = new FluidType(genericType, mediaType);
		return writer.isConsumable(ftype) && writer.nil(ftype).toMedia(accepter) != null;
	}

	public boolean isQueryStringPresent() {
		return request.getQueryString() != null;
	}

	private boolean isRequestBody(Method method) {
		for (Annotation[] anns : method.getParameterAnnotations()) {
			if (getParameterNames(anns) == null && getHeaderNames(anns) == null)
				return true;
		}
		return false;
	}

	private boolean isPrivate(Method method) {
		if (method == null)
			return false;
		for (Annotation[] anns : method.getParameterAnnotations()) {
			for (Annotation ann : anns) {
				if (ann.annotationType().equals(header.class)) {
					for (String hd : ((header)ann).value()) {
						if (hd.equalsIgnoreCase("Authorization") || hd.equalsIgnoreCase("Cookie"))
							return true;
					}
				}
			}
		}
		return false;
	}

	private String revisionTag(String version, boolean strong, int code) {
		if (version == null)
			return null;
		String revision = toHexString(version.hashCode());
		String weak = strong ? "" : "W/";
		if (code == 0)
			return weak + '"' + revision + '"';
		return weak + '"' + revision + '-' + toHexString(code) + '"';
	}

	private String variantTag(String version, boolean strong, String mediaType, int code) {
		if (mediaType == null)
			return revisionTag(version, strong, code);
		if (version == null)
			return null;
		String revision = toHexString(version.hashCode());
		String weak = strong ? "" : "W/";
		String cd = toHexString(code);
		String v = toHexString(mediaType.hashCode());
		if (code == 0)
			return weak + '"' + revision + '-' + v + '"';
		return weak + '"' + revision + '-' + cd + '-' + v + '"';
	}

	private Method findMethod() throws RepositoryException {
		Method method = findMethodIfPresent(request.getMethod(), request.isMessageBody(), null);
		if (method == null)
			throw new MethodNotAllowed("No such method for this resource");
		return method;
	}

	private Method findMethod(String req_method, Boolean isResponsePresent)
			throws RepositoryException {
		Method method = findMethodIfPresent(req_method, request.isMessageBody(), isResponsePresent);
		if (method == null)
			throw new MethodNotAllowed("No such method for this resource");
		return method;
	}

	private Method findMethodIfPresent(String req_method, boolean messageBody,
			Boolean isResponsePresent) {
		String name = getOperation();
		RDFObject target = getRequestedResource();
		if (name != null) {
			// lookup method
			List<Method> methods = getOperationMethods(req_method,
					isResponsePresent).get(name);
			if (methods != null) {
				Method method = findBestMethod(findAcceptableMethods(methods, messageBody));
				if (method != null)
					return method;
			}
		}
		List<Method> methods = new ArrayList<Method>();
		for (Method m : target.getClass().getMethods()) {
			if (m.isAnnotationPresent(ParameterTypes.class))
				continue;
			method ann = m.getAnnotation(method.class);
			if (ann == null)
				continue;
			if (!Arrays.asList(ann.value()).contains(req_method))
				continue;
			if (isOperationPresent(m))
				continue;
			if (name != null && isOperationProhibited(m))
				continue;
			boolean content = !m.getReturnType().equals(Void.TYPE);
			if (isResponsePresent != null && isResponsePresent != content)
				continue;
			methods.add(m);
		}
		if (!methods.isEmpty()) {
			Method method = findBestMethod(methods);
			if (method != null)
				return method;
		}
		return null;
	}

	private Method findBestMethod(Collection<Method> methods) {
		if (methods.isEmpty())
			return null;
		if (methods.size() == 1) {
			return methods.iterator().next();
		}
		FluidType acceptable = getAcceptable();
		Set<String> possible = new LinkedHashSet<String>();
		for (Method m : methods) {
			possible.addAll(getAllMimeTypesOf(m));
		}
		Map<String, Method> map = new LinkedHashMap<String, Method>();
		String[] mediaTypes = possible.toArray(new String[possible.size()]);
		FluidType ftype = new FluidType(acceptable.asType(), mediaTypes);
		String preferred = ftype.as(acceptable).preferred();
		for (Method m : methods) {
			possible.clear();
			possible.addAll(getAllMimeTypesOf(m));
			String[] media = possible.toArray(new String[possible.size()]);
			if (preferred == null || new FluidType(acceptable.asType(), media).is(preferred)) {
				String iri;
				Iri ann = m.getAnnotation(Iri.class);
				if (ann == null) {
					iri = m.toString();
				} else {
					iri = ann.value();
				}
				map.put(iri, m);
			}
		}
		if (map.size() == 1)
			return map.values().iterator().next();
		for (Method method : map.values().toArray(new Method[map.size()])) {
			for (String iri : getAnnotationStringValue(method, SUB_CLASS_OF)) {
				map.remove(iri);
			}
		}
		if (map.isEmpty())
			return null;
		return map.values().iterator().next();
	}

	private String[] getAnnotationStringValue(Method method, String iri) {
		for (Annotation ann : method.getAnnotations()) {
			for (Method field : ann.annotationType().getMethods()) {
				Iri airi = field.getAnnotation(Iri.class);
				if (airi != null && iri.equals(airi.value()))
					try {
						Object arg = field.invoke(ann);
						if (arg instanceof String[])
							return (String[]) arg;
						return new String[] { arg.toString() };
					} catch (IllegalArgumentException e) {
						logger.warn(e.toString(), e);
					} catch (InvocationTargetException e) {
						logger.warn(e.toString(), e);
					} catch (IllegalAccessException e) {
						logger.warn(e.toString(), e);
					}
			}
		}
		return new String[0];
	}

	private Collection<String> getAllMimeTypesOf(Method m) {
		if (m == null)
			return null;
		Collection<String> result = new LinkedHashSet<String>();
		if (m.isAnnotationPresent(type.class)) {
			for (String media : m.getAnnotation(type.class).value()) {
				result.add(media);
			}
		}
		if (result.isEmpty()) {
			result.add("*/*");
		}
		return result;
	}

	private Collection<Method> findAcceptableMethods(Collection<Method> methods, boolean messageBody) {
		String readable = null;
		String acceptable = null;
		Collection<Method> list = new LinkedHashSet<Method>(methods.size());
		Fluid body = getBody();
		loop: for (Method method : methods) {
			Collection<String> readableTypes;
			readableTypes = getReadableTypes(body, method, messageBody);
			if (readableTypes.isEmpty()) {
				String contentType = body.getFluidType().preferred();
				Annotation[][] anns = method.getParameterAnnotations();
				for (int i = 0; i < anns.length; i++) {
					String[] types = getParameterMediaTypes(anns[i]);
					Type gtype = method.getGenericParameterTypes()[i];
					if (body.toMedia(new FluidType(gtype, types)) == null) {
						if (contentType == null) {
							readable = "Cannot read unknown body into " + gtype;
						} else {
							readable = "Cannot read " + contentType + " into "
									+ gtype;
						}
						continue loop;
					}
				}
				if (readable == null && contentType != null) {
					readable = "Cannot read " + contentType;
				}
				if (readable != null) {
					continue loop;
				}
			}
			if (isAcceptable(method)) {
				list.add(method);
				continue loop;
			}
			acceptable = "Cannot write " + method.getGenericReturnType();
		}
		if (list.isEmpty() && readable != null) {
			throw new UnsupportedMediaType(readable);
		}
		if (list.isEmpty() && acceptable != null) {
			throw new NotAcceptable(acceptable);
		}
		return list;
	}

	private boolean isOperationPresent(Method m) {
		if (m.isAnnotationPresent(query.class)) {
			String[] values = m.getAnnotation(query.class).value();
			return values.length != 0 && (values.length != 1 || values[0].length() != 0);
		}
		return false;
	}

	private boolean isOperationProhibited(Method m) {
		if (m.isAnnotationPresent(query.class)) {
			String[] values = m.getAnnotation(query.class).value();
			return values.length == 0 || values.length == 1 && values[0].length() == 0;
		}
		return false;
	}

	public String[] getParameterNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(query.class))
				return ((query) annotations[i]).value();
		}
		return null;
	}

	public String[] getHeaderNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(header.class))
				return ((header) annotations[i]).value();
		}
		return null;
	}

	public String[] getParameterMediaTypes(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(type.class))
				return ((type) annotations[i]).value();
		}
		return new String[0];
	}

	private Map<String, List<Method>> getPostMethods(RDFObject target) {
		Map<String, List<Method>> map = new HashMap<String, List<Method>>();
		for (Method m : target.getClass().getMethods()) {
			if (m.isAnnotationPresent(ParameterTypes.class))
				continue;
			method ann = m.getAnnotation(method.class);
			if (ann == null) {
				if (m.isAnnotationPresent(query.class)
						&& !m.getReturnType().equals(Void.TYPE)
						&& isRequestBody(m)) {
					put(map, new String[] { "POST" }, m);
				}
			} else {
				put(map, ann.value(), m);
			}
		}
		return map;
	}

	private int getHeaderCodeFor(Method method) {
		if (method == null)
			return 0;
		Set<String> names = getHeaderNamesFor(method, new HashSet<String>());
		if (names.isEmpty())
			return 0;
		Map<String, String> headers = new HashMap<String, String>();
		for (String name : names) {
			Enumeration e = request.getHeaderEnumeration(name);
			while (e.hasMoreElements()) {
				String value = e.nextElement().toString();
				if (headers.containsKey(name)) {
					headers.put(name, headers.get(name) + "," + value);
				} else {
					headers.put(name, value);
				}
			}
		}
		return headers.hashCode();
	}

	private Set<String> getHeaderNamesFor(Method method, Set<String> names) {
		if (method == null)
			return names;
		for (Annotation[] anns : method.getParameterAnnotations()) {
			String[] ar = getHeaderNames(anns);
			if (ar != null) {
				names.addAll(Arrays.asList(ar));
			}
		}
		return names;
	}

	private boolean isAcceptable(Method method) {
		if (method == null)
			return false;
		if (method.getReturnType().equals(Void.TYPE))
			return true;
		if (method.isAnnotationPresent(expect.class)) {
			for (String expect : method.getAnnotation(expect.class).value()) {
				if (expect.startsWith("3"))
					return true; // redirection
				if (expect.startsWith("201") || expect.startsWith("202"))
					return true; // created
				if (expect.startsWith("204") || expect.startsWith("205"))
					return true; // no content
			}
		}
		return isAcceptable(method.getGenericReturnType(), getTypes(method));
	}

	private Collection<String> getReadableTypes(Fluid input, Annotation[] anns, Class<?> ptype,
			Type gtype, boolean typeRequired) {
		if (getHeaderNames(anns) != null)
			return Collections.singleton("*/*");
		if (getParameterNames(anns) != null)
			return Collections.singleton("*/*");
		List<String> readable = new ArrayList<String>();
		String[] types = getParameterMediaTypes(anns);
		if (types.length == 0 && typeRequired)
			return Collections.emptySet();
		String media = input.toMedia(new FluidType(gtype, types));
		if (media != null) {
			readable.add(media);
		}
		return readable;
	}

	private Collection<String> getReadableTypes(Fluid input,
			Method method, boolean typeRequired) {
		if (method == null)
			return Collections.emptySet();
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		if (args.length == 0 && !typeRequired)
			return Collections.singleton("*/*");
		int empty = 0;
		List<String> readable = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			Collection<String> set;
			set = getReadableTypes(input, anns[i], ptypes[i], gtypes[i],
					typeRequired);
			if (set.isEmpty()) {
				empty++;
			}
			if (getHeaderNames(anns[i]) == null
					&& getParameterNames(anns[i]) == null) {
				readable.addAll(set);
			}
		}
		if (empty > 0 && empty == args.length && typeRequired)
			return Collections.emptySet();
		if (readable.isEmpty() && !typeRequired)
			return Collections.singleton("*/*");
		return readable;
	}

	private void setCacheControl(Class<?> type, StringBuilder sb) {
		if (type.isAnnotationPresent(header.class)) {
			for (String value : type.getAnnotation(header.class).value()) {
				int idx = value.indexOf(':');
				if (idx < 0)
					continue;
				String name = value.substring(0, idx);
				if (name.equalsIgnoreCase("cache-control")) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(value.substring(idx + 1));
				}
			}
		} else {
			if (type.getSuperclass() != null) {
				setCacheControl(type.getSuperclass(), sb);
			}
			for (Class<?> face : type.getInterfaces()) {
				setCacheControl(face, sb);
			}
		}
	}

	private boolean noValidate(Class<?> type) {
		if (type.isAnnotationPresent(header.class)) {
			for (String value : type.getAnnotation(header.class).value()) {
				int idx = value.indexOf(':');
				if (idx < 0)
					continue;
				String name = value.substring(0, idx);
				if (!name.equalsIgnoreCase("cache-control"))
					continue;
				if (value.contains("no-validate"))
					return true;
				if (value.contains("must-reevaluate"))
					return true;
			}
		} else {
			if (type.getSuperclass() != null) {
				if (noValidate(type.getSuperclass()))
					return true;
			}
			for (Class<?> face : type.getInterfaces()) {
				if (noValidate(face))
					return true;
			}
		}
		return false;
	}

	private void put(Map<String, List<Method>> map, String[] keys, Method m) {
		for (String key : keys) {
			List<Method> list = map.get(key);
			if (list == null) {
				map.put(key, list = new ArrayList<Method>());
			}
			list.add(m);
		}
	}

	public String getRequestURL() {
		return request.getRequestURL();
	}

	public HttpEntity getEntity() {
		return request.getEntity();
	}

	public boolean containsHeader(String name) {
		return request.containsHeader(name);
	}

	public Header getFirstHeader(String name) {
		return request.getFirstHeader(name);
	}

	public long getDateHeader(String name) {
		return request.getDateHeader(name);
	}

	public Header[] getHeaders(String name) {
		return request.getHeaders(name);
	}

	public final boolean isSafe() {
		return request.isSafe();
	}

	public String getMethod() {
		return request.getMethod();
	}

	public String getIRI() {
		return request.getIRI();
	}

	public String getOrigin() {
		return request.getOrigin();
	}

	public String getRequestURI() {
		return request.getRequestURI();
	}

	public String resolve(String url) {
		return request.resolve(url);
	}

	public boolean isMessageBody() {
		return request.isMessageBody();
	}

	public String getScheme() {
		return request.getScheme();
	}

	public Enumeration getHeaderEnumeration(String name) {
		return request.getHeaderEnumeration(name);
	}

	public RequestLine getRequestLine() {
		return request.getRequestLine();
	}

}
