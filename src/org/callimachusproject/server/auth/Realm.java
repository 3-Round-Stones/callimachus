package org.callimachusproject.server.auth;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Realm {
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n";
	private static final String SELECT_REALM = PREFIX
			+ "SELECT ?forbidden ?unauthorized ?domain ?authentication ?authName (group_concat(?protected;separator=' ') as ?protected) {\n"
			+ "{ $this calli:authentication ?authentication . ?protected calli:authentication ?authentication\n"
			+ "OPTIONAL { ?authentication calli:authName ?authName } }"
			+ "UNION { $this calli:forbidden ?forbidden }\n"
			+ "UNION { $this calli:unauthorized ?unauthorized }\n"
			+ "UNION { $this a ?realm . ?realm calli:icon ?icon . ?domain a calli:Origin\n"
			+ "{ ?domain a ?realm } UNION { ?domain a [rdfs:subClassOf ?realm] }}\n"
			+ "} GROUP BY ?forbidden ?unauthorized ?domain ?authentication ?authName ORDER BY desc(?authName)";
	private static final BasicStatusLine _204;
	private static final BasicStatusLine _401;
	private static final BasicStatusLine _403;
	static {
		ProtocolVersion HTTP11 = HttpVersion.HTTP_1_1;
		_204 = new BasicStatusLine(HTTP11, 401, "No Content");
		_401 = new BasicStatusLine(HTTP11, 401, "Unauthorized");
		_403 = new BasicStatusLine(HTTP11, 403, "Forbidden");
	}

	private Logger logger = LoggerFactory.getLogger(Realm.class);
	private final List<AuthenticationManager> authentication = new ArrayList<AuthenticationManager>();
	private final Collection<String> allowOrigin = new LinkedHashSet<String>();
	private String forbidden;
	private String unauthorized;

	Realm(Resource self, ObjectConnection con) throws OpenRDFException {
		TupleQuery query = con.prepareTupleQuery(SPARQL, SELECT_REALM);
		query.setBinding("this", self);
		TupleQueryResult results = query.evaluate();
		try {
			while (results.hasNext()) {
				BindingSet result = results.next();
				if (result.hasBinding("forbidden")) {
					forbidden = result.getValue("forbidden").stringValue();
				}
				if (result.hasBinding("unauthorized")) {
					unauthorized = result.getValue("unauthorized").stringValue();
				}
				if (result.hasBinding("authName")) {
					String authName = result.getValue("authName").stringValue();
					Resource resource = (Resource) result.getValue("authentication");
					String domains = result.getValue("protected").stringValue();
					authentication.add(new DigestManager(resource, authName, domains));
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
		HttpResponse unauth = null;
		for (AuthenticationManager realm : getAuthenticationManagers()) {
			unauth = realm.unauthorized(method, resource, request);
			if (unauth != null)
				break;
		}
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
					resp.setHeader(hd);
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
			Map<String, String[]> request, ObjectConnection con) throws RepositoryException {
		for (AuthenticationManager realm : getAuthenticationManagers()) {
			String ret = realm.authenticateRequest(method, resource, request, con);
			if (ret != null)
				return ret;
		}
		return null;
	}

	public HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con) throws OpenRDFException {
		HttpMessage msg = new BasicHttpResponse(_204);
		for (AuthenticationManager realm : getAuthenticationManagers()) {
			HttpMessage resp = realm.authenticationInfo(method, resource, request, con);
			if (resp != null) {
				for (Header hd : resp.getAllHeaders()) {
					msg.addHeader(hd);
				}
			}
		}
		return msg;
	}

	public HttpResponse logout(Collection<String> tokens, String logoutContinue) throws IOException {
		BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1,
				303, "See Other");
		Iterator<AuthenticationManager> iter = authentication.iterator();
		while (iter.hasNext()) {
			AuthenticationManager manager = iter.next();
			HttpResponse logout = manager.logout(tokens);
			if (logout != null) {
				if (logout.getStatusLine().getStatusCode() >= 400) {
					return logout;
				}
				Header[] headers = logout.getAllHeaders();
				for (Header hd : headers) {
					resp.addHeader(hd);
				}
				if (logout.getEntity() != null) {
					EntityUtils.consume(logout.getEntity());
				}
			}
		}
		if (logoutContinue != null && logoutContinue.length() > 0) {
			resp.setHeader("Location", logoutContinue);
		} else {
			resp.setHeader("Location", "/");
		}
		return resp;
	}

	private Iterable<AuthenticationManager> getAuthenticationManagers() {
		List<AuthenticationManager> result = new ArrayList<AuthenticationManager>();
		Iterator<?> iter = authentication.iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			if (next instanceof AuthenticationManager) {
				result.add((AuthenticationManager) next);
			} else {
				logger.error("{} is not an AuthenticationManager", next);
			}
		}
		return result;
	}
}
