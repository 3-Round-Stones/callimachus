package org.callimachusproject.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.net.ParsedURI;

import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.concepts.AuthenticationManager;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.concepts.Realm;
import org.callimachusproject.traits.SelfAuthorizingTarget;
import org.openrdf.OpenRDFException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RealmSupport implements Realm, RDFObject {
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n";
	private static final BasicStatusLine _204;
	private static final BasicStatusLine _401;
	private static final BasicStatusLine _403;
	static {
		ProtocolVersion HTTP11 = new ProtocolVersion("HTTP", 1, 1);
		_204 = new BasicStatusLine(HTTP11, 401, "No Content");
		_401 = new BasicStatusLine(HTTP11, 401, "Unauthorized");
		_403 = new BasicStatusLine(HTTP11, 403, "Forbidden");
	}
	private Logger logger = LoggerFactory.getLogger(RealmSupport.class);

	public String allowOrigin() {
		String sparql = PREFIX
				+ "SELECT ?domain WHERE { ?domain a calli:Origin, ?origin . $this a ?realm\n"
				+ "FILTER contains(str(?realm),\"/callimachus/\")\n"
				+ "FILTER (?origin = ?realm || EXISTS { ?origin rdfs:subClassOf ?realm }) }";
		ObjectConnection con = getObjectConnection();
		try {
			TupleQuery tq = con.prepareTupleQuery(SPARQL, sparql);
			TupleQueryResult result = tq.evaluate();
			try {
				StringBuilder sb = new StringBuilder();
				while (result.hasNext()) {
					String uri = result.next().getValue("domain").stringValue();
					if (uri.contains("://")) {
						int idx = uri.indexOf('/', uri.indexOf("://") + 3);
						if (sb.length() > 0) {
							sb.append(',');
						}
						sb.append(uri, 0, idx);
					}
				}
				return sb.toString();
			} finally {
				result.close();
			}
		} catch (OpenRDFException e) {
			logger.error(e.toString(), e);
			return "";
		}
	}

	@Override
	public final boolean withAgentCredentials(String origin) {
		for (Object script : this.allowOrigin().split(",")) {
			String ao = script.toString();
			// must be explicitly listed ('*' does not qualify)
			if (origin.startsWith(ao) || ao.startsWith(origin)
					&& ao.charAt(origin.length()) == '/')
				return true;
		}
		return false;
	}

	@Override
	public final HttpResponse forbidden(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		Page forbidden = getCalliForbidden();
		if (forbidden == null)
			return null;
		String html = forbidden.calliConstructHTML(resource);
		StringEntity entity = new StringEntity(html, "UTF-8");
		entity.setContentType("text/html;charset=\"UTF-8\"");
		HttpResponse resp = new BasicHttpResponse(_403);
		resp.setHeader("Cache-Control", "no-store");
		resp.setEntity(entity);
		return resp;
	}

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, Map<String, String[]> request) {
		String query = new ParsedURI(request.get("request-target")[0]).getQuery();
		assert resource instanceof SelfAuthorizingTarget;
		SelfAuthorizingTarget target = (SelfAuthorizingTarget) resource;
		return target.calliIsAuthorized(credential, method, query);
	}

	@Override
	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		HttpResponse unauth = null;
		Page unauthorized = getCalliUnauthorized();
		for (AuthenticationManager realm : getCalliAuthentications()) {
			unauth = realm.unauthorized(method, resource, request);
			if (unauth != null)
				break;
		}
		if (unauthorized == null)
			return unauth;
		try {
			String html = unauthorized.calliConstructHTML(resource);
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
			StringEntity entity = new StringEntity(html, "UTF-8");
			entity.setContentType("text/html;charset=\"UTF-8\"");
			resp.setHeader("Cache-Control", "no-store");
			resp.setEntity(entity);
			return resp;
		} finally {
			if (unauth != null) {
				HttpEntity entity = unauth.getEntity();
				if (entity != null) {
					entity.consumeContent();
				}
			}
		}
	}

	@Override
	public Object authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException {
		for (AuthenticationManager realm : getCalliAuthentications()) {
			Object ret = realm.authenticateRequest(method, resource, request);
			if (ret != null)
				return ret;
		}
		return null;
	}

	@Override
	public HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request) {
		HttpMessage msg = new BasicHttpResponse(_204);
		for (AuthenticationManager realm : getCalliAuthentications()) {
			HttpMessage resp = realm.authenticationInfo(method, resource, request);
			if (resp != null) {
				for (Header hd : resp.getAllHeaders()) {
					msg.addHeader(hd);
				}
			}
		}
		return msg;
	}

}
