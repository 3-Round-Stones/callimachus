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
package org.callimachusproject.server.model;

import static java.lang.Integer.toHexString;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;

import org.callimachusproject.annotations.expect;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.rel;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.MethodNotAllowed;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.exceptions.UnsupportedMediaType;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.ParameterTypes;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class ResourceOperation extends ResourceRequest {
	private static final String SUB_CLASS_OF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
	private static int MAX_TRANSFORM_DEPTH = 100;
	private Logger logger = LoggerFactory.getLogger(ResourceOperation.class);

	private Method method;
	private MethodNotAllowed notAllowed;
	private BadRequest badRequest;
	private NotAcceptable notAcceptable;
	private UnsupportedMediaType unsupportedMediaType;
	private boolean isPublic;

	public ResourceOperation(Request request, CalliRepository repository)
			throws QueryEvaluationException, RepositoryException {
		super(request, repository);
	}

	public void begin() throws RepositoryException,
			QueryEvaluationException, DatatypeConfigurationException {
		super.begin();
		if (method == null) {
			try {
				String m = getMethod();
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
	}

	public String getResponseContentType() {
		if (method == null || method.getReturnType().equals(Void.TYPE))
			return null;
		return getContentType(method);
	}

	public String getEntityTag(String revision, String cache, String contentType)
			throws RepositoryException, QueryEvaluationException {
		Method m = this.method;
		int headers = getHeaderCodeFor(m);
		boolean strong = cache != null && cache.contains("cache-range");
		String method = getMethod();
		if (contentType != null) {
			return variantTag(revision, strong, contentType, headers);
		} else if ("GET".equals(method) || "HEAD".equals(method)) {
			if (m != null && contentType == null)
				return revisionTag(revision, strong, headers);
			if (m != null)
				return variantTag(revision, strong, contentType, headers);
			Method operation;
			if ((operation = getAlternativeMethod("alternate")) != null) {
				String type = getContentType(operation);
				headers = getHeaderCodeFor(operation);
				return variantTag(revision, strong, type, headers);
			} else if ((operation = getAlternativeMethod("describedby")) != null) {
				String type = getContentType(operation);
				headers = getHeaderCodeFor(operation);
				return variantTag(revision,strong,  type, headers);
			}
		} else {
			String putContentType = null;
			if ("PUT".equals(method)) {
				putContentType = getHeader("Content-Type");
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
				return revisionTag(revision, strong, headers);
			} else if (get == null) {
				return variantTag(revision, strong, putContentType, headers);
			} else {
				String get_cache = getResponseCacheControlFor(get);
				boolean get_strong = get_cache != null && get_cache.contains("cache-range");
				return variantTag(revision, get_strong, getContentType(get), headers);
			}
		}
		return null;
	}

	public boolean isNoValidate() {
		String method = getMethod();
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
		return super.getLastModified();
	}

	public String getResponseCacheControl() throws QueryEvaluationException,
			RepositoryException {
		return getResponseCacheControlFor(method);
	}

	private String getResponseCacheControlFor(Method m)
			throws QueryEvaluationException, RepositoryException {
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
		if (sb.indexOf("private") < 0 && sb.indexOf("public") < 0) {
			if (isPrivate(m)) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("private");
			} else if (!isPublic() && sb.indexOf("s-maxage") < 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("s-maxage=0");
			} else if (isPublic()) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("public");
			}
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

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public Set<String> getAllowedMethods() throws RepositoryException {
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

	private String revisionTag(String revision, boolean strong, int code) {
		if (revision == null)
			return null;
		String weak = strong ? "" : "W/";
		if (code == 0)
			return weak + '"' + revision + '"';
		return weak + '"' + revision + '-' + toHexString(code) + '"';
	}

	private String variantTag(String revision, boolean strong, String mediaType, int code) {
		if (mediaType == null)
			return revisionTag(revision, strong, code);
		if (revision == null)
			return null;
		String weak = strong ? "" : "W/";
		String cd = toHexString(code);
		String v = toHexString(mediaType.hashCode());
		if (code == 0)
			return weak + '"' + revision + '-' + v + '"';
		return weak + '"' + revision + '-' + cd + '-' + v + '"';
	}

	private Method findMethod() throws RepositoryException {
		Method method = findMethodIfPresent(getMethod(), isMessageBody(), null);
		if (method == null)
			throw new MethodNotAllowed("No such method for this resource");
		return method;
	}

	private Method findMethod(String req_method, Boolean isResponsePresent)
			throws RepositoryException {
		Method method = findMethodIfPresent(req_method, isMessageBody(), isResponsePresent);
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
			readableTypes = getReadableTypes(body, method, 0, messageBody);
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
			if (isAcceptable(method, 0)) {
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
			Enumeration e = getHeaderEnumeration(name);
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

	private boolean isAcceptable(Method method, int depth) {
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
		if (depth > MAX_TRANSFORM_DEPTH) {
			logger.error("Max transform depth exceeded: {}", method.getName());
			return false;
		}
		return isAcceptable(method.getGenericReturnType(), getTypes(method));
	}

	private Collection<String> getReadableTypes(Fluid input, Annotation[] anns, Class<?> ptype,
			Type gtype, int depth, boolean typeRequired) {
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
			Method method, int depth, boolean typeRequired) {
		if (method == null)
			return Collections.emptySet();
		if (depth > MAX_TRANSFORM_DEPTH) {
			logger.error("Max transform depth exceeded: {}", method.getName());
			return Collections.emptySet();
		}
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
			set = getReadableTypes(input, anns[i], ptypes[i], gtypes[i], depth,
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

}
