/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.auth;

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.exceptions.TooManyRequests;
import org.openrdf.http.object.helpers.ObjectContext;
import org.openrdf.http.object.util.URLUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationManager {
    public static final String CREDENTIAL_ATTR = AuthorizationManager.class.getName() + "#credential";
	public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static final BasicStatusLine _403 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 403, "Forbidden");
	private final Logger logger = LoggerFactory
			.getLogger(AuthenticationHandler.class);
	private final DomainNameSystemResolver dnsResolver = DomainNameSystemResolver.getInstance();
	private final AnnotationPropertyInferencer properties;
	private final GroupManager groupManager;
	private final RealmManager realmManager;

	public AuthorizationManager(RealmManager realmManager, ObjectRepository repository) {
		this.realmManager = realmManager;
		this.properties = new AnnotationPropertyInferencer(repository);
		this.groupManager = new GroupManager(repository);
	}

	public void resetCache() {
		properties.resetCache();
		groupManager.resetCache();
	}

	/**
	 * Called from composite.ttl when creating a new resource
	 */
	public boolean isAuthorized(String user, RDFObject target, String[] roles)
			throws OpenRDFException {
		Set<Group> groups = getAuthorizedParties(target, roles);
		return isPublic(groups) || isMember(user, groups);
	}

	public DetachedAuthenticationManager getAuthenticationManager(Resource uri)
			throws OpenRDFException, IOException {
		return realmManager.getAuthenticationManager(uri);
	}

	public Set<Group> getAuthorizedParties(RDFObject target, String[] requires) throws OpenRDFException,
			RepositoryException {
		Set<String> roles = properties.expand(requires);
		Set<String> parties = getAnnotationValuesOf(target, roles);
		return groupManager.getGroups(parties);
	}

	public boolean isPublic(Set<Group> groups) {
		for (Group group : groups) {
			if (group.isPublic())
				return true;
		}
		return false;
	}

	public HttpResponse authorize(HttpRequest request, Set<Group> groups, HttpContext context)
			throws OpenRDFException, IOException {
		ObjectContext ctx = ObjectContext.adapt(context);
		InetAddress clientAddr = ctx.getClientAddr();
		long now = ctx.getReceivedOn();
		String scheme = ctx.getProtocolScheme();
		HttpRequest oreq = ctx.getOriginalRequest();
		String m = (oreq == null ? request : oreq).getRequestLine().getMethod();
		RDFObject target = ctx.getResourceTarget().getTargetObject();
		Header[] or = request.getHeaders("Origin");
		Map<String, String[]> map = getAuthorizationMap(request, scheme, now, clientAddr, groups);
		List<String> from = getAgentFrom(map.get("via"));
		if (isAnonymousAllowed(from, groups))
			return null;
		// loop through first to see if further authorisation is needed
		DetachedRealm realm = getRealm(request, ctx);
		HttpResponse unauth = null;
		boolean validOrigin = false;
		boolean noRealm = true;
		if (realm != null) {
			String cred = null;
			Collection<String> allowed = realm.allowOrigin();
			try {
				if (or.length == 0 || isOriginAllowed(allowed, or)) {
					cred = (String) ctx.getAttribute(CREDENTIAL_ATTR);
					if (cred == null) {
						ObjectConnection con = target.getObjectConnection();
						cred = realm.authenticateRequest(m, target, map, con);
					}
					if (cred != null && isMember(cred, from, groups)) {
						ctx.setAttribute(CREDENTIAL_ATTR, cred);
						return null; // this request is good
					}
				}
			} catch (TooManyRequests e) {
				StringEntity body = new StringEntity(e.getDetailMessage(), Charset.forName("UTF-8"));
				body.setContentType("text/plain");
				BasicStatusLine line = new BasicStatusLine(HttpVersion.HTTP_1_1, e.getStatusCode(), e.getShortMessage());
				HttpResponse resp = new BasicHttpResponse(line);
				resp.setHeader("Content-Type", "text/plain;charset=UTF-8");
				for (Header hd : e.getResponseHeaders()) {
					resp.addHeader(hd);
				}
				resp.setEntity(body);
				return resp;
			}
			noRealm = false;
			validOrigin = or.length == 0 || isOriginAllowed(allowed, or);
			try {
				if (cred == null && validOrigin) {
					unauth = choose(unauth,
							realm.unauthorized(m, target, map, getRequestEntity(request)));
				} else if (validOrigin) {
					unauth = choose(unauth, realm.forbidden(m, target, map));
				} else {
					unauth = choose(unauth, realm.notAllowed(m, target, map));
				}
			} catch (ResponseException exc) {
				if (unauth != null) {
					EntityUtils.consumeQuietly(unauth.getEntity());
				}
				throw exc;
			} catch (Exception exc) {
				logger.error(exc.toString(), exc);
			}
		}
		if (noRealm) {
			logger.info("No active realm for {}", request);
		} else if (!validOrigin) {
			logger.info("Origin {} not allowed for {}", or, request);
		}
		if (unauth != null)
			return unauth;
		StringEntity body = new StringEntity("Forbidden", Charset.forName("UTF-8"));
		body.setContentType("text/plain");
		HttpResponse resp = new BasicHttpResponse(_403);
		resp.setHeader("Content-Type", "text/plain;charset=UTF-8");
		resp.setEntity(body);
		return resp;
	}

	public HttpMessage authenticationInfo(HttpRequest request, HttpContext context)
			throws IOException, OpenRDFException {
		ObjectContext ctx = ObjectContext.adapt(context);
		DetachedRealm realm = getRealm(request, ctx);
		if (realm == null)
			return null;
		InetAddress clientAddr = ctx.getClientAddr();
		long now = ctx.getReceivedOn();
		String scheme = ctx.getProtocolScheme();
		HttpRequest oreq = ctx.getOriginalRequest();
		String m = (oreq == null ? request : oreq).getRequestLine().getMethod();
		RDFObject target = ctx.getResourceTarget().getTargetObject();
		Map<String, String[]> map = getAuthorizationMap(request, scheme, now, clientAddr, null);
		return realm.authenticationInfo(m, target, map, target.getObjectConnection());
	}

	public boolean withAgentCredentials(HttpRequest request, HttpContext context,
			String origin) throws OpenRDFException, IOException {
		DetachedRealm realm = getRealm(request, ObjectContext.adapt(context));
		return realm != null && realm.withAgentCredentials(origin);
	}

	public Set<String> allowOrigin(HttpRequest request, HttpContext context)
			throws OpenRDFException, IOException {
		Set<String> set = new LinkedHashSet<String>();
		DetachedRealm realm = getRealm(request, ObjectContext.adapt(context));
		if (realm != null) {
			set.addAll(realm.allowOrigin());
		}
		return set;
	}

	private HttpEntity getRequestEntity(HttpRequest request) {
		if (request instanceof HttpEntityEnclosingRequest)
			return ((HttpEntityEnclosingRequest) request).getEntity();
		return null;
	}

	private Set<String> getAnnotationValuesOf(RDFObject target, Set<String> roles) throws OpenRDFException {
		if (roles.isEmpty())
			return Collections.emptySet();
		Class<?> cls = target.getClass();
		HashSet<String> set = new HashSet<String>();
		getAnnotationValues(cls, roles, set);
		ObjectConnection con = target.getObjectConnection();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT REDUCED ?value {{");
		String union = "} UNION {";
		boolean found = false;
		for (String role : roles) {
			if (role.indexOf(':') > 0 && role.indexOf('>') < 0) {
				sb.append("$target <").append(role).append("> ?value");
				sb.append(union);
				found = true;
			}
		}
		if (!found)
			return set;
		sb.setLength(sb.length() - union.length());
		sb.append("}}");
		String q = sb.toString();
		TupleQuery qry = con.prepareTupleQuery(QueryLanguage.SPARQL, q);
		qry.setBinding("target", target.getResource());
		TupleQueryResult results = qry.evaluate();
		try {
			while (results.hasNext()) {
				Value value = results.next().getValue("value");
				set.add(value.stringValue());
			}
		} finally {
			results.close();
		}
		return set;
	}

	private void getAnnotationValues(Class<?> cls, Set<String> roles,
			Set<String> set) {
		for (Annotation ann : cls.getAnnotations()) {
			try {
				Method value = ann.annotationType().getMethod("value");
				Iri iri = value.getAnnotation(Iri.class);
				if (iri != null && roles.contains(iri.value())) {
					Object obj = value.invoke(ann);
					if (obj instanceof String[]) {
						set.addAll(Arrays.asList((String[]) obj));
					}
				}
			} catch (NoSuchMethodException e) {
				continue;
			} catch (IllegalAccessException e) {
				continue;
			} catch (IllegalArgumentException e) {
				logger.error(e.toString(), e);
			} catch (InvocationTargetException e) {
				logger.error(e.toString(), e);
			}
		}
		for (Class<?> face : cls.getInterfaces()) {
			getAnnotationValues(face, roles, set);
		}
		if (cls.getSuperclass() != null) {
			getAnnotationValues(cls.getSuperclass(), roles, set);
		}
	}

	private DetachedRealm getRealm(HttpRequest request, ObjectContext ctx)
			throws OpenRDFException, IOException {
		String url = getRequestURI(request, ctx);
		DetachedRealm realm = realmManager.getRealm(URLUtil.canonicalize(url));
		if (realm == null) {
			return realmManager.getRealm(url);
		}
		return realm;
	}

	private String getRequestURI(HttpRequest request, ObjectContext ctx) {
		String path = request.getRequestLine().getUri();
		try {
			int qx = path.indexOf('?');
			if (qx > 0) {
				path = path.substring(0, qx);
			}
			if (!path.startsWith("/"))
				return path;
			String scheme = ctx.getProtocolScheme().toLowerCase();
			String host = getAuthority(request).toLowerCase();
			return new ParsedURI(scheme, host, path, null, null).toString();
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e);
		}
	}

	private String getAuthority(HttpRequest request) {
		String uri = request.getRequestLine().getUri();
		if (uri != null && !uri.equals("*") && !uri.startsWith("/")) {
			try {
				String authority = new java.net.URI(uri).getAuthority();
				if (authority != null)
					return authority;
			} catch (URISyntaxException e) {
				// try the host header
			}
		}
		Header host = request.getLastHeader("Host");
		if (host != null)
			return host.getValue().toLowerCase();
		throw new BadRequest("Missing Host Header for request-uri: " + uri);
	}

	private List<String> getAgentFrom(String[] sources) {
		List<String> list = new ArrayList<String>(sources.length);
		for (String via : sources) {
			int start = via.indexOf(' ');
			if (start > 0) {
				int end = via.indexOf(':', start + 1);
				if (end < 0) {
					end = via.indexOf(' ', start + 1);
				}
				if (end < 0) {
					end = via.length();
				}
				String host = via.substring(start + 1, end);
				list.add(host);
			}
		}
		return list;
	}

	private boolean isAnonymousAllowed(List<String> from, Set<Group> groups) {
		if (from == null)
			return false;
		loop: for (Group group : groups) {
			for (String host : from) {
				if (!group.isAnonymousAllowed(host))
					continue loop;
			}
			return true;
		}
		return false;
	}

	private boolean isMember(String user, Set<Group> groups) {
		for (Group group : groups) {
			if (group.isEveryoneAllowed() || group.isMember(user))
				return true;
		}
		return false;
	}

	private boolean isMember(String user, List<String> from, Set<Group> groups) {
		if (from != null && isEveryoneAllowed(from, groups))
			return true;
		loop: for (Group group : groups) {
			if (from == null) {
				if (group.isMember(user))
					return true;
			} else {
				for (String host : from) {
					if (!group.isMember(user, host))
						continue loop;
				}
				return true;
			}
		}
		return false;
	}

	private boolean isEveryoneAllowed(List<String> from, Set<Group> groups) {
		if (from == null)
			return false;
		loop: for (Group group : groups) {
			for (String host : from) {
				if (!group.isEveryoneAllowed(host))
					continue loop;
			}
			return true;
		}
		return false;
	}

	private HttpResponse choose(HttpResponse a, HttpResponse b)
			throws IOException {
		if (a == null)
			return b;
		if (b == null)
			return a;
		int acode = a.getStatusLine().getStatusCode();
		int bcode = b.getStatusLine().getStatusCode();
		// prefer 401 Unauthorized responses
		if (bcode < acode && (bcode == 401 || acode != 401)) {
			EntityUtils.consume(a.getEntity());
			return b;
		} else {
			EntityUtils.consume(b.getEntity());
			return a;
		}
	}

	private Map<String, String[]> getAuthorizationMap(HttpRequest request,
			String scheme, long now, InetAddress clientAddr, Set<Group> groups) {
		Map<String, String[]> map = new HashMap<String, String[]>();
		map.put("request-target", new String[] { request.getRequestLine().getUri() });
		map.put("request-scheme", new String[] { scheme });
		map.put("date", new String[] { DateUtils.formatDate(new Date(now)) });
		Header[] au = request.getHeaders("Authorization");
		if (au != null && au.length > 0) {
			map.put("authorization", toStringArray(au));
		}
		Header[] co = request.getHeaders("Cookie");
		if (co != null && co.length > 0) {
			map.put("cookie", toStringArray(co));
		}
		Header[] ho = request.getHeaders("Host");
		if (ho != null && ho.length > 0) {
			map.put("host", toStringArray(ho));
		}
		String via = getRequestSource(request, clientAddr, groups);
		map.put("via", via.split("\\s*,\\s*"));
		return map;
	}

	private String[] toStringArray(Header[] au) {
		String[] result = new String[au.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = au[i].getValue();
		}
		return result;
	}

	private String getRequestSource(HttpRequest request,
			InetAddress clientAddr, Set<Group> groups) {
		StringBuilder via = new StringBuilder();
		for (Header hd : request.getHeaders("X-Forwarded-For")) {
			for (String ip : hd.getValue().split("\\s*,\\s*")) {
				if (via.length() > 0) {
					via.append(",");
				}
				via.append("1.1 ").append(getHostName(ip, groups));
			}
		}
		for (Header hd : request.getHeaders("Via")) {
			if (via.length() > 0) {
				via.append(",");
			}
			via.append(hd.getValue());
		}
		if (via.length() > 0) {
			via.append(",");
		}
		via.append("1.1 ").append(getHostName(clientAddr, groups));
		return via.toString();
	}

	private String getHostName(String ip, Set<Group> groups) {
		InetAddress clientAddr = dnsResolver.getByName(ip);
		if (clientAddr == null)
			return ip;
		return getHostName(clientAddr, groups);
	}

	private String getHostName(InetAddress clientAddr, Set<Group> groups) {
		if (dnsResolver.isNetworkLocalAddress(clientAddr))
			return dnsResolver.reverse(clientAddr);
		if (groups != null) {
			for (Group group : groups) {
				if (group.isHostReferenced(clientAddr)) {
					return dnsResolver.reverse(clientAddr);
				}
			}
		}
		return dnsResolver.getArpaName(clientAddr);
	}

	private boolean isOriginAllowed(Collection<String> allowed, Header[] origin) {
		if (allowed == null)
			return false;
		for (String ao : allowed) {
			for (Header hd : origin) {
				String o = hd.getValue();
				if (ao.equals("*") || o.startsWith(ao) || ao.startsWith(o)
						&& ao.charAt(o.length()) == '/')
					return true;
			}
		}
		return false;
	}

}
