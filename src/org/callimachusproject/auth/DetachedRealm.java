package org.callimachusproject.auth;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.tools.FileObject;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.concepts.AuthenticationManager;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetachedRealm {
	private static final String PREF_AUTH = "prefAuth=";
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n";
	private static final String SELECT_REALM = PREFIX
			+ "SELECT ?secret ?forbidden ?unauthorized ?domain ?authentication (group_concat(?protected;separator=' ') as ?protected) {\n"
			+ "{ $this calli:authentication ?authentication . ?protected calli:authentication ?authentication }\n"
			+ "UNION { $this calli:secret ?secret }\n"
			+ "UNION { $this calli:forbidden ?forbidden }\n"
			+ "UNION { $this calli:unauthorized ?unauthorized }\n"
			+ "UNION { $this a ?realm . ?realm calli:icon ?icon . ?domain a calli:Origin\n"
			+ "{ ?domain a ?realm } UNION { ?domain a [rdfs:subClassOf ?realm] }}\n"
			+ "} GROUP BY ?secret ?forbidden ?unauthorized ?domain ?authentication";
	private static final BasicStatusLine _204;
	private static final BasicStatusLine _401;
	private static final BasicStatusLine _403;
	static {
		ProtocolVersion HTTP11 = HttpVersion.HTTP_1_1;
		_204 = new BasicStatusLine(HTTP11, 401, "No Content");
		_401 = new BasicStatusLine(HTTP11, 401, "Unauthorized");
		_403 = new BasicStatusLine(HTTP11, 403, "Forbidden");
	}

	public static String getPreferredManagerCookie(String realm, String manager) {
		String path = new ParsedURI(realm).getPath();
		try {
			String value = URLEncoder.encode(manager, "UTF-8");
			return PREF_AUTH + value + ";Path=" + path + ";HttpOnly";
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private Logger logger = LoggerFactory.getLogger(DetachedRealm.class);
	private final Map<Resource, DetachedAuthenticationManager> authentication = new HashMap<Resource, DetachedAuthenticationManager>();
	private final Collection<String> allowOrigin = new LinkedHashSet<String>();
	private String secret;
	private String forbidden;
	private String unauthorized;

	public DetachedRealm(Resource self, ObjectConnection con, RealmManager manager)
			throws OpenRDFException {
		TupleQuery query = con.prepareTupleQuery(SPARQL, SELECT_REALM);
		query.setBinding("this", self);
		TupleQueryResult results = query.evaluate();
		try {
			while (results.hasNext()) {
				BindingSet result = results.next();
				if (result.hasBinding("secret")) {
					URI uri = (URI) result.getValue("secret");
					secret = readString(con.getBlobObject(uri));
				}
				if (result.hasBinding("forbidden")) {
					forbidden = result.getValue("forbidden").stringValue();
				}
				if (result.hasBinding("unauthorized")) {
					unauthorized = result.getValue("unauthorized")
							.stringValue();
				}
				if (result.hasBinding("authentication")) {
					Value protects = result.getValue("protected");
					Value uri = result.getValue("authentication");
					DetachedAuthenticationManager dam = detach((Resource) uri,
							protects.stringValue(), manager, con);
					if (dam != null) {
						authentication.put((Resource) uri, dam);
					}
				}
				if (result.hasBinding("domain")) {
					String uri = result.getValue("domain").stringValue();
					if (uri.contains("://")) {
						int idx = uri.indexOf('/', uri.indexOf("://") + 3);
						if (idx > 0) {
							allowOrigin.add(uri.substring(0, idx));
						}
					}
				}
			}
		} finally {
			results.close();
		}
	}

	public String toString() {
		return allowOrigin.toString();
	}

	public DetachedAuthenticationManager getAuthenticationManager(Resource uri) {
		return authentication.get(uri);
	}

	public String getSecret() {
		return secret;
	}

	public Collection<String> allowOrigin() {
		return allowOrigin;
	}

	public final boolean withAgentCredentials(String origin) {
		for (Object script : this.allowOrigin()) {
			String ao = script.toString();
			// must be explicitly listed ('*' does not qualify)
			if (origin.startsWith(ao) || ao.startsWith(origin)
					&& ao.charAt(origin.length()) == '/')
				return true;
		}
		return false;
	}

	public final HttpResponse forbidden(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		if (forbidden == null)
			return null;
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpEntity entity = client.get(forbidden, "text/html;charset=UTF-8")
				.getEntity();
		HttpResponse resp = new BasicHttpResponse(_403);
		resp.setHeader("Cache-Control", "no-store");
		resp.setEntity(entity);
		return resp;
	}

	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		HttpResponse unauth = unauthorizedHeaders(method, resource, request);
		if (unauthorized == null)
			return unauth;
		try {
			HTTPObjectClient client = HTTPObjectClient.getInstance();
			HttpEntity entity = client.get(unauthorized,
					"text/html;charset=UTF-8").getEntity();
			BasicHttpResponse resp;
			if (unauth == null) {
				resp = new BasicHttpResponse(_401);
			} else {
				resp = new BasicHttpResponse(unauth.getStatusLine());
				for (Header hd : unauth.getAllHeaders()) {
					if (hd.getName().equalsIgnoreCase("Transfer-Encoding"))
						continue;
					if (hd.getName().toLowerCase().startsWith("content-"))
						continue;
					resp.addHeader(hd);
				}
			}
			resp.setHeader("Cache-Control", "no-store");
			resp.setEntity(entity);
			return resp;
		} finally {
			if (unauth != null) {
				EntityUtils.consume(unauth.getEntity());
			}
		}
	}

	public String authenticateRequest(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con)
			throws RepositoryException {
		for (DetachedAuthenticationManager realm : getManagers(request.get("cookie"))) {
			String ret = realm.authenticateRequest(method, resource, request,
					con);
			if (ret != null)
				return ret;
		}
		return null;
	}

	public HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con)
			throws OpenRDFException {
		HttpMessage msg = new BasicHttpResponse(_204);
		for (DetachedAuthenticationManager realm : getManagers(request.get("cookie"))) {
			HttpMessage resp = realm.authenticationInfo(method, resource,
					request, con);
			if (resp != null) {
				for (Header hd : resp.getAllHeaders()) {
					msg.addHeader(hd);
				}
			}
		}
		return msg;
	}

	public HttpResponse logout(Collection<String> tokens, String logoutContinue)
			throws IOException {
		BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1,
				303, "See Other");
		Iterator<DetachedAuthenticationManager> iter = authentication.values()
				.iterator();
		while (iter.hasNext()) {
			DetachedAuthenticationManager manager = iter.next();
			HttpResponse logout = manager.logout(tokens);
			if (logout != null) {
				if (logout.getStatusLine().getStatusCode() >= 400) {
					return logout;
				}
				Header[] headers = logout.getAllHeaders();
				for (Header hd : headers) {
					resp.addHeader(hd);
				}
				EntityUtils.consume(logout.getEntity());
			}
		}
		if (logoutContinue != null && logoutContinue.length() > 0) {
			resp.setHeader("Location", logoutContinue);
		} else {
			resp.setHeader("Location", "/");
		}
		return resp;
	}

	private String readString(FileObject file) {
		try {
			Reader reader = file.openReader(true);
			if (reader == null)
				return null;
			try {
				return new Scanner(reader).next();
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return null;
		}
	}

	private HttpResponse unauthorizedHeaders(String method, Object resource,
			Map<String, String[]> request) throws IOException {
		HttpResponse unauth = null;
		for (DetachedAuthenticationManager realm : getPrefManagers(request.get("cookie"))) {
			HttpResponse resp = realm.unauthorized(method, resource, request);
			if (resp == null)
				continue;
			if (unauth == null) {
				unauth = resp;
			} else {
				int code = resp.getStatusLine().getStatusCode();
				int unauthCode = unauth.getStatusLine().getStatusCode();
				if (code >= 400 && unauthCode < 400) {
					unauth = copyAuthHeaders(unauth, resp);
				} else {
					unauth = copyAuthHeaders(resp, unauth);
				}
			}
		}
		return unauth;
	}

	private HttpResponse copyAuthHeaders(HttpResponse source,
			HttpResponse destination) throws IOException {
		try {
			for (Header hd : source.getHeaders("WWW-Authenticate")) {
				destination.addHeader(hd);
			}
			for (Header hd : source.getHeaders("Set-Cookie")) {
				destination.addHeader(hd);
			}
			return destination;
		} finally {
			EntityUtils.consume(source.getEntity());
		}
	}

	private Iterable<DetachedAuthenticationManager> getManagers(String[] cookies) {
		String preferred = getPreferredManager(cookies);
		List<DetachedAuthenticationManager> result = new ArrayList<DetachedAuthenticationManager>();
		Iterator<?> iter = authentication.values().iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			if (next instanceof DetachedAuthenticationManager) {
				if (next.toString().equals(preferred)) {
					result.add(0, (DetachedAuthenticationManager) next);
				} else {
					result.add((DetachedAuthenticationManager) next);
				}
			} else {
				logger.error("{} is not an AuthenticationManager", next);
			}
		}
		return result;
	}

	private Iterable<DetachedAuthenticationManager> getPrefManagers(String[] cookies) {
		String preferred = getPreferredManager(cookies);
		List<DetachedAuthenticationManager> result = new ArrayList<DetachedAuthenticationManager>();
		Iterator<?> iter = authentication.values().iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			if (next instanceof DetachedAuthenticationManager) {
				if (((DetachedAuthenticationManager) next).getIdentifier().equals(preferred)) {
					return Collections.singleton((DetachedAuthenticationManager) next);
				} else {
					result.add((DetachedAuthenticationManager) next);
				}
			} else {
				logger.error("{} is not an AuthenticationManager", next);
			}
		}
		return result;
	}

	private String getPreferredManager(String[] cookies) {
		if (cookies == null)
			return null;
		for (String cookie : cookies) {
			if (!cookie.contains(PREF_AUTH))
				continue;
			String[] pair = cookie.split("\\s*;\\s*");
			for (String p : pair) {
				if (p.startsWith(PREF_AUTH)) {
					String encoded = p.substring(PREF_AUTH.length());
					try {
						return URLDecoder.decode(encoded, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw new AssertionError(e);
					}
				}
			}
		}
		return null;
	}

	private DetachedAuthenticationManager detach(Resource resource,
			String protects, RealmManager manager, ObjectConnection con)
			throws RepositoryException, OpenRDFException {
		List<String> domains = getDistinctRealm(protects);
		String path = getCommonPath(domains);
		Object am = con.getObject(resource);
		try {
			return ((AuthenticationManager) am).detachAuthenticationManager(
					path, domains, manager);
		} catch (ClassCastException e) {
			logger.error(e.toString() + " on " + resource);
			return null;
		} catch (AbstractMethodError e) {
			logger.error(e.toString() + " on " + resource);
			return null;
		}
	}

	private List<String> getDistinctRealm(String domains) {
		List<String> realms = new ArrayList<String>();
		if (domains.contains(" ")) {
			loop: for (String url : domains.split("\\s+")) {
				if (url.length() > 0) {
					for (int i = 0, n = realms.size(); i < n; i++) {
						String pre = realms.get(i);
						if (pre == null || pre.startsWith(url)) {
							realms.set(i, url);
							continue loop;
						} else if (url.startsWith(pre)) {
							continue loop;
						}
					}
					realms.add(url);
				}
			}
			return realms;
		} else {
			return Collections.singletonList(domains);
		}
	}

	private String getCommonPath(List<String> domains) {
		String pre = null;
		for (String url : domains) {
			String path = new ParsedURI(url).getPath();
			if (path != null && path.startsWith("/")) {
				if (pre == null || pre.startsWith(path)) {
					pre = path;
				}
				while (!path.startsWith(pre)) {
					int slash = pre.lastIndexOf('/', pre.length() - 2);
					pre = pre.substring(0, slash);
				}
			}
		}
		return pre == null ? "/" : pre;
	}
}
