package org.callimachusproject.server.auth;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
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

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.server.handlers.AuthenticationHandler;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;
import org.openrdf.model.Value;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationManager {
	private static final AuthorizationManager instance = new AuthorizationManager();

	public static AuthorizationManager getInstance() {
		return instance;
	}

	public static void reset() {
		AnnotationPropertyInferencer.reset();
		GroupManager.reset();
		RealmManager.reset();
	}

	public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static final BasicStatusLine _403 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 403, "Forbidden");
	private final Logger logger = LoggerFactory
			.getLogger(AuthenticationHandler.class);
	private final DomainNameSystemResolver dnsResolver = DomainNameSystemResolver.getInstance();
	private final AnnotationPropertyInferencer properties = new AnnotationPropertyInferencer();
	private final GroupManager groupManager = new GroupManager();
	private final RealmManager realmManager = new RealmManager();

	public void resetCache() {
		properties.resetCache();
		groupManager.resetCache();
		realmManager.resetCache();
	}

	/**
	 * Called from composite.ttl when creating a new resource
	 */
	public boolean isAuthorized(String user, RDFObject target, String[] roles)
			throws RepositoryException, OpenRDFException {
		Set<Group> groups = getAuthorizedParties(target, roles);
		return isPublic(groups) || isMember(user, groups);
	}

	public Realm getRealm(String target, Repository repo)
			throws OpenRDFException {
		return realmManager.getRealm(target, repo);
	}

	public Set<Group> getAuthorizedParties(RDFObject target, String[] requires) throws OpenRDFException,
			RepositoryException {
		Repository repo = target.getObjectConnection().getRepository();
		Set<String> roles = properties.expand(requires, repo);
		Set<String> parties = getAnnotationValuesOf(target, roles);
		return groupManager.getGroups(parties, repo);
	}

	public boolean isPublic(Set<Group> groups) {
		for (Group group : groups) {
			if (group.isPublic())
				return true;
		}
		return false;
	}

	public HttpResponse authorize(ResourceOperation request, Set<Group> groups)
			throws OpenRDFException, IOException {
		String m = request.getMethod();
		RDFObject target = request.getRequestedResource();
		String or = request.getVaryHeader("Origin");
		Map<String, String[]> map = getAuthorizationMap(request);
		List<String> from = getAgentFrom(map.get("via"));
		if (isAnonymousAllowed(from, groups))
			return null;
		// loop through first to see if further authorisation is needed
		Realm realm = getRealm(request);
		HttpResponse unauth = null;
		boolean wrongOrigin = true;
		boolean noRealm = true;
		if (realm != null) {
			String cred = null;
			Collection<String> allowed = realm.allowOrigin();
			if (or == null || isOriginAllowed(allowed, or)) {
				ObjectConnection con = request.getObjectConnection();
				cred = realm.authenticateRequest(m, target, map, con);
				if (cred != null && isMember(cred, from, groups)) {
					request.setCredential(cred);
					return null; // this request is good
				}
			}
			noRealm = false;
			if (or != null && !isOriginAllowed(allowed, or)) {
				try {
					unauth = choose(unauth, realm.forbidden(m, target, map));
				} catch (Exception exc) {
					logger.error(exc.toString(), exc);
				}
			} else {
				wrongOrigin = false;
				try {
					if (cred == null) {
						unauth = choose(unauth,
								realm.unauthorized(m, target, map));
					} else {
						unauth = choose(unauth, realm.forbidden(m, target, map));
					}
				} catch (Exception exc) {
					logger.error(exc.toString(), exc);
				}
			}
		}
		if (noRealm) {
			logger.info("No active realm for {}", request);
		} else if (wrongOrigin) {
			logger.info("Origin {} not allowed for {}", or, request);
		}
		if (unauth != null)
			return unauth;
		StringEntity body = new StringEntity("Forbidden", "UTF-8");
		body.setContentType("text/plain");
		HttpResponse resp = new BasicHttpResponse(_403);
		resp.setHeader("Content-Type", "text/plain;charset=UTF-8");
		resp.setEntity(body);
		return resp;
	}

	public HttpMessage authenticationInfo(ResourceOperation request)
			throws IOException, OpenRDFException {
		Realm realm = getRealm(request);
		if (realm == null)
			return null;
		String m = request.getMethod();
		RDFObject target = request.getRequestedResource();
		Map<String, String[]> map = getAuthorizationMap(request);
		return realm.authenticationInfo(m, target, map, request.getObjectConnection());
	}

	public boolean withAgentCredentials(ResourceOperation request,
			String origin) throws OpenRDFException {
		Realm realm = getRealm(request);
		return realm != null && realm.withAgentCredentials(origin);
	}

	public Set<String> allowOrigin(ResourceOperation request)
			throws OpenRDFException {
		Set<String> set = new LinkedHashSet<String>();
		Realm realm = getRealm(request);
		if (realm == null && request.isPublic()) {
			return Collections.singleton("*");
		} else if (realm != null) {
			set.addAll(realm.allowOrigin());
		}
		return set;
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

	private Realm getRealm(ResourceOperation request) throws OpenRDFException {
		Repository repo = request.getObjectConnection().getRepository();
		Realm realm = getRealm(request.getIRI(), repo);
		if (realm == null)
			return getRealm(request.getRequestURI(), repo);
		return realm;
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

	private HttpResponse choose(HttpResponse unauthorized, HttpResponse auth)
			throws IOException {
		if (unauthorized == null)
			return auth;
		if (auth == null)
			return unauthorized;
		int code = unauthorized.getStatusLine().getStatusCode();
		if (auth.getStatusLine().getStatusCode() < code) {
			EntityUtils.consume(unauthorized.getEntity());
			return auth;
		} else {
			EntityUtils.consume(auth.getEntity());
			return unauthorized;
		}
	}

	private Map<String, String[]> getAuthorizationMap(ResourceOperation request)
			throws IOException {
		long now = request.getReceivedOn();
		Map<String, String[]> map = new HashMap<String, String[]>();
		map.put("request-target", new String[] { request.getRequestTarget() });
		map.put("request-scheme", new String[] { request.getScheme() });
		map.put("date", new String[] { DateUtil.formatDate(new Date(now)) });
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
		String via = getRequestSource(request);
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

	private String getRequestSource(ResourceOperation request) {
		StringBuilder via = new StringBuilder();
		for (String hd : request.getVaryHeaders("X-Forwarded-For")) {
			for (String ip : hd.split("\\s*,\\s*")) {
				if (via.length() > 0) {
					via.append(",");
				}
				via.append("1.1 ").append(dnsResolver.reverse(ip));
			}
		}
		for (String hd : request.getVaryHeaders("Via")) {
			if (via.length() > 0) {
				via.append(",");
			}
			via.append(hd);
		}
		InetAddress remoteAddr = request.getRemoteAddr();
		if (via.length() > 0) {
			via.append(",");
		}
		via.append("1.1 ").append(dnsResolver.reverse(remoteAddr));
		return via.toString();
	}

	private boolean isOriginAllowed(Collection<String> allowed, String o) {
		if (allowed == null)
			return false;
		for (String ao : allowed) {
			if (ao.equals("*") || o.startsWith(ao) || ao.startsWith(o)
					&& ao.charAt(o.length()) == '/')
				return true;
		}
		return false;
	}

}
