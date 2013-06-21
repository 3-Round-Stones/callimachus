package org.callimachusproject.auth;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.tools.FileObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.HttpClientFactory;
import org.callimachusproject.client.HttpUriClient;
import org.callimachusproject.concepts.AuthenticationManager;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.xproc.Pipe;
import org.callimachusproject.xproc.Pipeline;
import org.callimachusproject.xproc.PipelineFactory;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.store.blob.BlobObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetachedRealm {
	private static final String PREF_AUTH = "prefAuth";
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n";
	private static final String SELECT_REALM = PREFIX
			+ "SELECT ?secret ?error ?forbidden ?unauthorized ?domain ?authentication ?protected ?username ?passwordFile ?authority {\n"
			+ "{ $this calli:credentials [calli:username ?username; calli:passwordFile ?passwordFile; calli:authority ?authority] }\n"
			+ "UNION { $this calli:authentication ?authentication . ?protected calli:authentication ?authentication }\n"
			+ "UNION { $origin calli:secret ?secret }\n"
			+ "UNION { $this calli:error ?error }\n"
			+ "UNION { $this calli:forbidden ?forbidden }\n"
			+ "UNION { $this calli:unauthorized ?unauthorized }\n"
			+ "UNION { $this calli:allowOrigin ?domain }\n"
			+ "}";
	private static final ThreadLocal<Boolean> inForbidden = new ThreadLocal<Boolean>();
	private static final ThreadLocal<Boolean> inUnauthorized = new ThreadLocal<Boolean>();
	private static final int MAX_PRETTY_CONCURRENT_ERRORS = Runtime.getRuntime().availableProcessors();
	private static final ThreadLocal<Boolean> inError = new ThreadLocal<Boolean>();
	private static final AtomicInteger activeErrors = new AtomicInteger();
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
		ParsedURI parsed = new ParsedURI(realm);
		String path = parsed.getPath();
		try {
			String value = URLEncoder.encode(manager, "UTF-8");
			StringBuilder sb = new StringBuilder();
			sb.append(PREF_AUTH);
			String auth = parsed.getAuthority();
			if (auth.contains(":")) {
				sb.append(auth.substring(auth.lastIndexOf(':') + 1));
			}
			if ("https".equalsIgnoreCase(parsed.getScheme())) {
				sb.append("s");
			}
			sb.append('=');
			sb.append(value);
			sb.append(";Max-Age=2678400;Path=");
			sb.append(path);
			sb.append(";HttpOnly");
			if ("https".equalsIgnoreCase(parsed.getScheme())) {
				sb.append(";Secure");
			}
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private Logger logger = LoggerFactory.getLogger(DetachedRealm.class);
	private final Map<Resource, DetachedAuthenticationManager> authentication = new HashMap<Resource, DetachedAuthenticationManager>();
	private final Collection<String> allowOrigin = new LinkedHashSet<String>();
	private final CredentialsProvider credentials = new SystemDefaultCredentialsProvider();
	private final Resource self;
	private HttpUriClient client;
	private String secret;
	private Pipeline error;
	private String forbidden;
	private String unauthorized;

	public DetachedRealm(Resource self) {
		this.self = self;
	}

	public void init(ObjectConnection con, RealmManager manager)
			throws OpenRDFException, IOException {
		credentials.clear();
		String errorPipelineUrl = null;
		Map<Value, String> protects = new LinkedHashMap<Value, String>();
		ValueFactory vf = con.getValueFactory();
		TupleQuery query = con.prepareTupleQuery(SPARQL, SELECT_REALM);
		query.setBinding("this", self);
		String origin = TermFactory.newInstance(self.stringValue()).resolve("/");
		query.setBinding("origin", vf.createURI(origin));
		TupleQueryResult results = query.evaluate();
		try {
			while (results.hasNext()) {
				BindingSet result = results.next();
				if (result.hasBinding("secret")) {
					URI uri = (URI) result.getValue("secret");
					secret = readString(con.getBlobObject(uri));
				}
				if (result.hasBinding("error")) {
					errorPipelineUrl = result.getValue("error").stringValue();
				}
				if (result.hasBinding("forbidden")) {
					forbidden = result.getValue("forbidden").stringValue();
				}
				if (result.hasBinding("unauthorized")) {
					unauthorized = result.getValue("unauthorized")
							.stringValue();
				}
				if (result.hasBinding("authentication")) {
					Value uri = result.getValue("authentication");
					String value = result.getValue("protected").stringValue();
					if (protects.containsKey(uri)) {
						protects.put(uri, protects.get(uri) + ' ' + value);
					} else {
						protects.put(uri, value);
					}
				}
				if (result.hasBinding("domain")) {
					allowOrigin.add(result.getValue("domain").stringValue());
				}
				if (result.hasBinding("username")) {
					String username = result.getValue("username").stringValue();
					String authority = result.getValue("authority").stringValue();
					String passwordFile = result.getValue("passwordFile").stringValue();
					setCredential(username, authority, passwordFile, con);
				}
			}
		} finally {
			results.close();
		}
		this.client = new HttpUriClient() {
			private final CloseableHttpClient delegate = HttpClientFactory
					.getInstance().createHttpClient(self.stringValue(),
							credentials);

			protected HttpClient getDelegate() throws IOException {
				return delegate;
			}
		};
		if (errorPipelineUrl != null) {
			error = PipelineFactory.newInstance().createPipeline(errorPipelineUrl, client);
		}
		for (Map.Entry<Value, String> e : protects.entrySet()) {
			Resource uri = (Resource) e.getKey();
			DetachedAuthenticationManager dam = detach(uri, e.getValue(),
					manager, con);
			if (dam != null) {
				authentication.put(uri, dam);
			}
		}
	}

	public Resource getResource() {
		return self;
	}

	public String toString() {
		return getResource().stringValue();
	}

	public DetachedAuthenticationManager getAuthenticationManager(Resource uri) {
		return authentication.get(uri);
	}

	public String getOriginSecret() {
		return secret;
	}

	public HttpUriClient getHttpClient() {
		return client;
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

	public void transformErrorPage(String xhtml, Writer writer, String target, String query) throws IOException {
		if (error != null && inError.get() == null
				&& activeErrors.get() < MAX_PRETTY_CONCURRENT_ERRORS) {
			String id = error.getSystemId();
			if (id == null || !id.equals(target)) {
				try {
					inError.set(true);
					activeErrors.incrementAndGet();
					Pipe pb = error.pipeReader(new StringReader(xhtml), target);
					try {
						pb.passOption("target", target);
						pb.passOption("query", query);
						String body = pb.asString();
						writer.append(body);
						return;
					} finally {
						pb.close();
					}
				} catch (Throwable exc) {
					logger.error(exc.toString(), exc);
				} finally {
					inError.remove();
					activeErrors.decrementAndGet();
				}
			}
		}
		writer.write(xhtml);
	}

	public final HttpResponse forbidden(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		if (forbidden == null)
			return null;
		HttpResponse resp = new BasicHttpResponse(_403);
		resp.setHeader("Cache-Control", "no-store");
		try {
			if (inForbidden.get() == null) {
				inForbidden.set(true);
				HttpEntity entity = client.getEntity(forbidden, "text/html;charset=UTF-8");
				resp.setEntity(entity);
			} else {
				resp.setEntity(new StringEntity("Forbidden"));
			}
			return resp;
		} finally {
			inForbidden.remove();
		}
	}

	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> req, HttpEntity body)
			throws Exception {
		HttpResponse unauth = unauthorizedHeaders(method, resource, req, body);
		if (unauthorized == null)
			return unauth;
		try {
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
			if (inUnauthorized.get() == null) {
				inUnauthorized.set(true);
				HttpEntity entity = client.getEntity(unauthorized,
						"text/html;charset=UTF-8");
				resp.setEntity(entity);
			} else {
				String via = Arrays.asList(req.get("via")).toString();
				resp.setEntity(new StringEntity(via + " is unauthorized"));
			}
			return resp;
		} finally {
			inUnauthorized.remove();
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
			throws OpenRDFException, IOException {
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

	private void setCredential(String username, String authority,
			String passwordFile, ObjectConnection con) throws IOException,
			RepositoryException, UnsupportedEncodingException {
		BlobObject file = con.getBlobObject(passwordFile);
		String encoded = file.getCharContent(true).toString();
		String password = new String(Base64.decodeBase64(encoded), "UTF-8");
		String host = authority;
		int port = -1;
		int index = authority.indexOf(':');
		if (index >= 0) {
			host = authority.substring(0, index);
			port = Integer.parseInt(authority.substring(index + 1));
		}
		AuthScope scope = new AuthScope(new HttpHost(host, port));
		NTCredentials credential = new NTCredentials(username + ':' + password);
		credentials.setCredentials(scope, credential);
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
			Map<String, String[]> request, HttpEntity body) throws IOException {
		HttpResponse unauth = null;
		for (DetachedAuthenticationManager realm : getPrefManagers(request.get("cookie"))) {
			HttpResponse resp = realm.unauthorized(method, resource, request, body);
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
		Set<String> preferred = getPreferredManager(cookies);
		List<DetachedAuthenticationManager> result = new ArrayList<DetachedAuthenticationManager>();
		Iterator<?> iter = authentication.values().iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			if (next instanceof DetachedAuthenticationManager) {
				DetachedAuthenticationManager manager = (DetachedAuthenticationManager) next;
				if (preferred.contains(manager.getIdentifier())) {
					result.add(0, manager);
				} else {
					result.add(manager);
				}
			} else {
				logger.error("{} is not an AuthenticationManager", next);
			}
		}
		return result;
	}

	private Iterable<DetachedAuthenticationManager> getPrefManagers(String[] cookies) {
		Set<String> preferred = getPreferredManager(cookies);
		List<DetachedAuthenticationManager> result = new ArrayList<DetachedAuthenticationManager>();
		Iterator<?> iter = authentication.values().iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			if (next instanceof DetachedAuthenticationManager) {
				DetachedAuthenticationManager dam = (DetachedAuthenticationManager) next;
				if (preferred.contains(dam.getIdentifier())) {
					return Collections.singleton(dam);
				} else {
					result.add(dam);
				}
			} else {
				logger.error("{} is not an AuthenticationManager", next);
			}
		}
		return result;
	}

	private Set<String> getPreferredManager(String[] cookies) {
		if (cookies == null)
			return Collections.emptySet();
		Set<String> set = new LinkedHashSet<String>();
		for (String cookie : cookies) {
			if (!cookie.contains(PREF_AUTH))
				continue;
			String[] pair = cookie.split("\\s*;\\s*");
			for (String p : pair) {
				if (p.startsWith(PREF_AUTH)) {
					String encoded = p.substring(p.indexOf('=') + 1);
					try {
						set.add(URLDecoder.decode(encoded, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new AssertionError(e);
					}
				}
			}
		}
		return set;
	}

	private DetachedAuthenticationManager detach(Resource resource,
			String protects, RealmManager manager, ObjectConnection con)
			throws OpenRDFException {
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
