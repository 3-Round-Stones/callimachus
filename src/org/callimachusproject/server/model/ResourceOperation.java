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
import static org.callimachusproject.server.util.Accepter.isCompatible;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.tools.FileObject;
import javax.xml.datatype.XMLGregorianCalendar;

import org.callimachusproject.annotations.expect;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.realm;
import org.callimachusproject.annotations.rel;
import org.callimachusproject.annotations.transform;
import org.callimachusproject.annotations.type;
import org.callimachusproject.concepts.Realm;
import org.callimachusproject.server.concepts.Transaction;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.MethodNotAllowed;
import org.callimachusproject.server.exceptions.NotAcceptable;
import org.callimachusproject.server.exceptions.UnsupportedMediaType;
import org.callimachusproject.server.util.Accepter;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.ParameterTypes;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
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
	private static final MimeType ANYTHING;
	static {
		try {
			ANYTHING = new MimeType("*", "*");
		} catch (MimeTypeParseException e) {
			throw new AssertionError(e);
		}
	}
	private static int MAX_TRANSFORM_DEPTH = 100;
	private Logger logger = LoggerFactory.getLogger(ResourceOperation.class);

	private Method method;
	private Method transformMethod;
	private MethodNotAllowed notAllowed;
	private BadRequest badRequest;
	private NotAcceptable notAcceptable;
	private UnsupportedMediaType unsupportedMediaType;
	private List<Realm> realms;
	private String[] realmURIs;

	public ResourceOperation(Request request, ObjectRepository repository)
			throws QueryEvaluationException, RepositoryException,
			MimeTypeParseException {
		super(request, repository);
	}

	public void begin() throws MimeTypeParseException, RepositoryException,
			QueryEvaluationException {
		super.begin();
		if (method == null) {
			try {
				String m = getMethod();
				if ("GET".equals(m) || "HEAD".equals(m)) {
					method = findMethod("GET", true);
				} else if ("PUT".equals(m) || "DELETE".equals(m)) {
					method = findMethod(m, false);
				} else {
					method = findMethod(m);
				}
				transformMethod = getTransformMethodOf(method);
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

	public String getResponseContentType() throws MimeTypeParseException {
		Method m = getTransformMethod();
		if (m == null || m.getReturnType().equals(Void.TYPE))
			return null;
		return getContentType(m);
	}

	public String getEntityTag(String revision, String cache, String contentType)
			throws MimeTypeParseException, RepositoryException, QueryEvaluationException {
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
				String type = getContentType(getTransformMethodOf(operation));
				headers = getHeaderCodeFor(operation);
				return variantTag(revision, strong, type, headers);
			} else if ((operation = getAlternativeMethod("describedby")) != null) {
				String type = getContentType(getTransformMethodOf(operation));
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
				get = findMethodIfPresent("GET", true);
				if (get != null) {
					headers = getHeaderCodeFor(get);
					get = getTransformMethodOf(get);
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

	public Class<?> getEntityType() throws MimeTypeParseException {
		String method = getMethod();
		Method m = getTransformMethod();
		if (m == null || "PUT".equals(method) || "DELETE".equals(method)
				|| "OPTIONS".equals(method))
			return null;
		return m.getReturnType();
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

	public long getLastModified() throws MimeTypeParseException {
		if (isNoValidate())
			return System.currentTimeMillis() / 1000 * 1000;
		RDFObject target = getRequestedResource();
		try {
			if (target instanceof FileObject)
				return ((FileObject) target).getLastModified() / 1000 * 1000;
			Transaction trans = getRevision();
			if (trans != null) {
				XMLGregorianCalendar xgc = trans.getCommittedOn();
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
			} else if (isAuthenticating() && sb.indexOf("s-maxage") < 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("s-maxage=0");
			} else if (!isAuthenticating()) {
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

	public boolean isAuthenticating() throws QueryEvaluationException,
			RepositoryException {
		return getRealmURIs().length > 0;
	}

	public List<Realm> getRealms() throws QueryEvaluationException,
			RepositoryException {
		if (realms != null)
			return realms;
		String[] values = getRealmURIs();
		return realms = getRealms(values);
	}

	public List<Realm> getRealms(String[] values)
			throws QueryEvaluationException, RepositoryException {
		if (values.length == 0)
			return Collections.emptyList();
		List<String> array = new ArrayList<String>(values.length);
		String iri = getIRI();
		for (String v : values) {
			if (iri.startsWith(v)) {
				array.add(v);
			}
		}
		if (array.size() > 1) {
			Collections.sort(array, new Comparator<String>() {
				public int compare(String o1, String o2) {
					return o2.length() < o1.length() ? -1 : (o2.length() == o1
							.length() ? 0 : 1);
				}
			});
		}
		if (array.size() < values.length) {
			for (String v : values) {
				if (!iri.startsWith(v)) {
					array.add(v);
				}
			}
		}
		String[] a = array.toArray(new String[array.size()]);
		ObjectConnection con = getObjectConnection();
		return con.getObjects(Realm.class, a).asList();
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

	public Method getAlternativeMethod(String rel) throws MimeTypeParseException {
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
			return findBestMethod(findAcceptableMethods(methods));
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
			if (query == null || query.length == 0 || query[0] == null
					|| query[0].length() == 0)
				continue;
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

	public Object[] getParameters(Method method, Entity input) throws Exception {
		Class<?>[] ptypes = method.getParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		Object[] args = new Object[ptypes.length];
		for (int i = 0; i < args.length; i++) {
			String[] types = getParameterMediaTypes(anns[i]);
			args[i] = getParameter(anns[i], ptypes[i], input).read(ptypes[i],
					gtypes[i], types);
		}
		return args;
	}

	public ResponseEntity invoke(Method method, Object[] args, boolean follow)
			throws Exception {
		Object result = method.invoke(getRequestedResource(), args);
		ResponseEntity input = createResultEntity(result, method
				.getReturnType(), method.getGenericReturnType(),
				getTypes(method));
		if (method.isAnnotationPresent(header.class)) {
			for (String header : method.getAnnotation(header.class).value()) {
				int idx = header.indexOf(':');
				if (idx <= 0)
					continue;
				String name = header.substring(0, idx);
				String value = header.substring(idx + 1);
				input.addHeader(name, value);
			}
		}
		if (method.isAnnotationPresent(expect.class)) {
			input.addExpects(method.getAnnotation(expect.class).value());
		}
		if (follow) {
			Method transform = getBestTransformMethod(method);
			if (transform != null && !transform.equals(method)) {
				ResponseEntity ret = invoke(transform, getParameters(transform, input), follow);
				ret.addHeaders(input.getOtherHeaders());
				ret.addExpects(input.getExpects());
				return ret;
			}
		}
		return input;
	}

	public boolean isRequestBody(Method method) {
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
		if (method.isAnnotationPresent(transform.class)) {
			for (String uri : method.getAnnotation(transform.class).value()) {
				if (isPrivate(getTransform(uri)))
					return true;
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

	private String[] getTypes(Method method) {
		if (method.isAnnotationPresent(type.class))
			return method.getAnnotation(type.class).value();
		return new String[0];
	}

	private Method findMethod(String method) throws MimeTypeParseException, RepositoryException {
		return findMethod(method, null);
	}

	private Method findMethod(String req_method, Boolean isResponsePresent)
			throws MimeTypeParseException, RepositoryException {
		Method method = findMethodIfPresent(req_method, isResponsePresent);
		if (method == null)
			throw new MethodNotAllowed("No such method for this resource");
		return method;
	}

	private Method findMethodIfPresent(String req_method,
			Boolean isResponsePresent) throws MimeTypeParseException {
		String name = getOperation();
		RDFObject target = getRequestedResource();
		if (name != null) {
			// lookup method
			List<Method> methods = getOperationMethods(req_method,
					isResponsePresent).get(name);
			if (methods != null) {
				Method method = findBestMethod(findAcceptableMethods(methods));
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

	private Method findBestMethod(Collection<Method> methods)
			throws MimeTypeParseException {
		if (methods.isEmpty())
			return null;
		if (methods.size() == 1) {
			Method method = methods.iterator().next();
			if (isAcceptable(method, 0))
				return method;
			return null;
		}
		for (MimeType accept : getAcceptable()) {
			Map<String, Method> best = new LinkedHashMap<String, Method>();
			for (Method m : methods) {
				for (String type : getAllMimeTypesOf(m)) {
					MimeType server = Accepter.parse(type);
					if (!best.containsKey(server.toString()) && isCompatible(accept, server)) {
						best.put(server.toString(), m);
					}
				}
			}
			if (!best.isEmpty()) {
				Accepter accepter = new Accepter(best.keySet());
				return best.get(accepter.getAcceptable().first().toString());
			}
		}
		return null;

	}

	private Collection<String> getAllMimeTypesOf(Method m) throws MimeTypeParseException {
		if (m == null)
			return null;
		Collection<String> result = new LinkedHashSet<String>();
		if (m.isAnnotationPresent(transform.class)) {
			for (String uri : m.getAnnotation(transform.class).value()) {
				result.addAll(getAllMimeTypesOf(getTransform(uri)));
			}
		}
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

	private Collection<Method> findAcceptableMethods(Collection<Method> methods)
			throws MimeTypeParseException {
		String readable = null;
		String acceptable = null;
		Collection<Method> list = new LinkedHashSet(methods.size());
		BodyEntity body = getBody();
		loop: for (Method method : methods) {
			Collection<? extends MimeType> readableTypes;
			readableTypes = getReadableTypes(body, method, 0, true);
			if (readableTypes.isEmpty()) {
				String contentType = body.getContentType();
				Annotation[][] anns = method.getParameterAnnotations();
				for (int i = 0; i < anns.length; i++) {
					String[] types = getParameterMediaTypes(anns[i]);
					Accepter accepter = new Accepter(types);
					if (!accepter.isAcceptable(contentType)) {
						if (contentType == null) {
							readable = "Cannot read unknown body into "
									+ method.getGenericParameterTypes()[i];
						} else {
							readable = "Cannot read " + contentType + " into "
									+ method.getGenericParameterTypes()[i];
						}
						continue loop;
					}
				}
				if (readable == null && contentType == null) {
					readable = "All request parameters must be a query parameter, header, or have a content type";
				} else if (readable == null) {
					readable = "Cannot read " + contentType;
				}
				continue loop;
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

	private Entity getParameter(Annotation[] anns, Class<?> ptype, Entity input)
			throws Exception {
		String[] names = getParameterNames(anns);
		String[] headers = getHeaderNames(anns);
		String[] types = getParameterMediaTypes(anns);
		if (names == null && headers == null) {
			return getValue(anns, input);
		} else if (headers != null && names != null) {
			return getValue(anns, getHeaderAndQuery(types, headers, names));
		} else if (headers != null) {
			return getValue(anns, getHeader(types, headers));
		} else if (names.length == 1 && names[0].equals("*")) {
			return getValue(anns, getQueryString(types));
		} else {
			return getValue(anns, getParameter(types, names));
		}
	}

	private Entity getValue(Annotation[] anns, Entity input) throws Exception {
		for (String uri : getTransforms(anns)) {
			Method transform = getTransform(uri);
			if (!getReadableTypes(input, transform, 0, false).isEmpty()) {
				Object[] args = getParameters(transform, input);
				return invoke(transform, args, false);
			}
		}
		return input;
	}

	private String[] getParameterNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(query.class))
				return ((query) annotations[i]).value();
		}
		return null;
	}

	private String[] getHeaderNames(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(header.class))
				return ((header) annotations[i]).value();
		}
		return null;
	}

	private String[] getParameterMediaTypes(Annotation[] annotations) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i].annotationType().equals(type.class))
				return ((type) annotations[i]).value();
		}
		return null;
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

	private Method getTransform(String uri) {
		for (Method m : getRequestedResource().getClass().getMethods()) {
			if (m.isAnnotationPresent(Iri.class)) {
				if (uri.equals(m.getAnnotation(Iri.class).value())) {
					return m;
				}
			}
		}
		logger.warn("Method not found: {}", uri);
		return null;
	}

	private Method getTransformMethod() {
		return transformMethod;
	}

	private Method getTransformMethodOf(Method method)
			throws MimeTypeParseException {
		Method transform = getBestTransformMethod(method);
		if (transform == null || transform.equals(method))
			return method;
		return getTransformMethodOf(transform);
	}

	private Method getBestTransformMethod(Method method) throws MimeTypeParseException {
		if (method == null)
			return method;
		if (method.isAnnotationPresent(transform.class)) {
			List<Method> transforms = new ArrayList<Method>();
			for (String uri : method.getAnnotation(transform.class).value()) {
				transforms.add(getTransform(uri));
			}
			Method tm = findBestMethod(transforms);
			if (tm == null)
				return method;
			return tm;
		}
		return method;
	}

	private String[] getTransforms(Annotation[] anns) {
		for (Annotation ann : anns) {
			if (ann.annotationType().equals(transform.class)) {
				return ((transform) ann).value();
			}
		}
		return new String[0];
	}

	private int getHeaderCodeFor(Method method) throws MimeTypeParseException {
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

	private Set<String> getHeaderNamesFor(Method method, Set<String> names)
			throws MimeTypeParseException {
		for (Annotation[] anns : method.getParameterAnnotations()) {
			String[] ar = getHeaderNames(anns);
			if (ar != null) {
				names.addAll(Arrays.asList(ar));
			}
		}
		if (method.isAnnotationPresent(transform.class)) {
			for (String uri : method.getAnnotation(transform.class).value()) {
				getHeaderNamesFor(getTransform(uri), names);
			}
		}
		return names;
	}

	private boolean isAcceptable(Method method, int depth)
			throws MimeTypeParseException {
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
		return getAcceptable(method, depth) != null;
	}

	private String getAcceptable(Method method, int depth)
			throws MimeTypeParseException {
		if (method == null)
			return null;
		if (depth > MAX_TRANSFORM_DEPTH) {
			logger.error("Max transform depth exceeded: {}", method.getName());
			return null;
		}
		if (method.isAnnotationPresent(transform.class)) {
			for (String uri : method.getAnnotation(transform.class).value()) {
				String str = getAcceptable(getTransform(uri), ++depth);
				if (str != null)
					return str;
			}
		}
		if (method.isAnnotationPresent(type.class)) {
			for (String media : getTypes(method)) {
				if (isAcceptable(media, method.getReturnType(), method
						.getGenericReturnType()))
					return media;
			}
			return null;
		} else if (isAcceptable(method.getReturnType(), method
				.getGenericReturnType())) {
			return "*/*";
		} else {
			return null;
		}
	}

	private Collection<? extends MimeType> getReadableTypes(Entity input, Annotation[] anns, Class<?> ptype,
			Type gtype, int depth, boolean typeRequired) throws MimeTypeParseException {
		if (getHeaderNames(anns) != null)
			return Collections.singleton(ANYTHING);
		if (getParameterNames(anns) != null)
			return Collections.singleton(ANYTHING);
		Collection<? extends MimeType> set;
		List<MimeType> readable = new ArrayList<MimeType>();
		String[] types = getParameterMediaTypes(anns);
		if (types == null && typeRequired)
			return Collections.emptySet();
		for (String uri : getTransforms(anns)) {
			set = getReadableTypes(input, getTransform(uri), ++depth, false);
			readable.addAll(set);
		}
		Accepter accepter = new Accepter(types);
		set = input.getReadableTypes(ptype, gtype, accepter);
		readable.addAll(accepter.getCompatible(set));
		return readable;
	}

	private Collection<? extends MimeType> getReadableTypes(Entity input,
			Method method, int depth, boolean typeRequired)
			throws MimeTypeParseException {
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
		if (args.length == 0)
			return Collections.singleton(ANYTHING);
		Collection<? extends MimeType> set;
		List<MimeType> readable = new ArrayList<MimeType>();
		for (int i = 0; i < args.length; i++) {
			set = getReadableTypes(input, anns[i], ptypes[i], gtypes[i], depth,
					typeRequired);
			if (set.isEmpty())
				return Collections.emptySet();
			if (getHeaderNames(anns[i]) == null
					&& getParameterNames(anns[i]) == null) {
				readable.addAll(set);
			}
		}
		if (readable.isEmpty())
			return Collections.singleton(ANYTHING);
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

	private String[] getRealmURIs() {
		if (realmURIs != null)
			return realmURIs;
		RDFObject target = getRequestedResource();
		if (method != null && method.isAnnotationPresent(realm.class)) {
			realmURIs = method.getAnnotation(realm.class).value();
		} else {
			ArrayList<String> list = new ArrayList<String>();
			addRealms(list, target.getClass());
			realmURIs = list.toArray(new String[list.size()]);
		}
		return realmURIs;
	}

	private void addRealms(ArrayList<String> list, Class<?> type) {
		if (type.isAnnotationPresent(realm.class)) {
			for (String value : type.getAnnotation(realm.class).value()) {
				list.add(value);
			}
		} else {
			if (type.getSuperclass() != null) {
				addRealms(list, type.getSuperclass());
			}
			for (Class<?> face : type.getInterfaces()) {
				addRealms(list, face);
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
