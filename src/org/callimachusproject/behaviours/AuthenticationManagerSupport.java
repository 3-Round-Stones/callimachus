/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.behaviours;

import static org.callimachusproject.util.PercentCodec.decode;
import static org.callimachusproject.util.PercentCodec.encode;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.DetachedAuthenticationManager;
import org.callimachusproject.concepts.AuthenticationManager;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;

public abstract class AuthenticationManagerSupport implements CalliObject,
		AuthenticationManager {
	private static final int MAX_NONCE_AGE = 300000; // nonce timeout of 5min
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String DERIVED_FROM = "http://www.w3.org/ns/prov#wasDerivedFrom";
	private static final String FOAF_DEPICTION = "http://xmlns.com/foaf/0.1/depiction";
	private static final String HAS_COMPONENT = CALLI+"hasComponent";
	private static final String PARTY = CALLI+"Party";
	private static final String USER = CALLI+"User";
	private static final String EMAIL = CALLI+"email";
	private static final String PROV = "http://www.w3.org/ns/prov#";
	private static final String PREFIX = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String IDENTITY_NAME_EMAIL = PREFIX + "SELECT ?name ?email { $identity rdfs:label ?name; calli:email ?email }";

	public HttpResponse openIDProvider(String parameters)
			throws ParseException, OpenRDFException, IOException,
			GeneralSecurityException {
		Map<String, String> map = keyByName(parameters);
		String mode = map.get("openid.mode");
		if ("associate".equals(mode)) {
			String body = "ns=http://specs.openid.net/auth/2.0&error=use+direct+verification&error_code=unsupported-type";
			BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 400, "Unsupported");
			resp.setHeader("Content-Type", "text/plain");
			resp.setEntity(new StringEntity(body));
			return resp;
		} else if ("checkid_setup".equals(mode)) {
			String self = this.getResource().stringValue();
			String return_to = self + "?checkid&" + parameters;
			if (!map.containsKey("openid.return_to")) {
				return_to = "/";
			}
			String location = self + "?login&return_to=" + encode(return_to);
			BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 303, "See Other");
			resp.setHeader("Location", location);
			resp.setHeader("Content-Type", "text/plain");
			resp.setEntity(new StringEntity(location));
			return resp;
		} else if ("checkid_immediate".equals(mode)) {
			String self = this.getResource().stringValue();
			String location = self + "?checkid&" + parameters;
			BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 303, "See Other");
			resp.setHeader("Location", location);
			resp.setHeader("Content-Type", "text/plain");
			resp.setEntity(new StringEntity(location));
			return resp;
		} else if ("check_authentication".equals(mode)) {
		    String user_fullname = map.get("openid.ax.value.fullname");
		    String user_email = map.get("openid.ax.value.email");
		    String user_uri = map.get("openid.identity");
		    String response_nonce = map.get("openid.response_nonce");
		    String time = response_nonce.substring(0, response_nonce.indexOf('Z') + 1);
		    String self = this.getResource().stringValue();
			StringBuilder hash = new StringBuilder(user_uri);
		    if (user_fullname != null && user_email != null) {
			    hash.append(':').append(user_fullname).append(':').append(user_email);
		    }
		    String nonce = time + hash(hash.toString(), "MD5");
		    StringBuilder sig = new StringBuilder();
			sig.append("openid.op_endpoint=").append(encode(self));
			sig.append("&openid.identity=").append(encode(user_uri));
			sig.append("&openid.claimed_id=").append(encode(user_uri));
			sig.append("&openid.return_to=").append(encode(map.get("openid.return_to")));
			sig.append("&openid.response_nonce=").append(encode(nonce));
		    Date now = new Date();
		    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		    df.setTimeZone(TimeZone.getTimeZone("UTC"));
		    Date then = df.parse(time);
			if (!nonce.equals(response_nonce)) {
				String body = "ns:http://specs.openid.net/auth/2.0\nis_valid:false";
				BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 400, "Bad openid.response_nonce");
				resp.setHeader("Content-Type", "text/plain");
				resp.setEntity(new StringEntity(body));
				return resp;
			} else if (!sig(sig.toString()).equals(map.get("openid.sig"))) {
				String body = "ns:http://specs.openid.net/auth/2.0\nis_valid:false";
				BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 400, "Bad openid.sig");
				resp.setHeader("Content-Type", "text/plain");
				resp.setEntity(new StringEntity(body));
				return resp;
			} else if (then.getTime() + MAX_NONCE_AGE < now.getTime()) {
				String body = "ns:http://specs.openid.net/auth/2.0\nis_valid:false";
				BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 400, "Request Has Expired");
				resp.setHeader("Content-Type", "text/plain");
				resp.setEntity(new StringEntity(body));
				return resp;
			} else {
				String body = "ns:http://specs.openid.net/auth/2.0\nis_valid:true";
				BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
				resp.setHeader("Content-Type", "text/plain");
				resp.setHeader("Cache-Control", "no-store");
				resp.setEntity(new StringEntity(body));
				return resp;
			}
		} else {
			String location = this.getResource().stringValue() + "?view";
			BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 303, "See Other");
			resp.setHeader("Location", location);
			resp.setHeader("Content-Type", "text/plain");
			resp.setEntity(new StringEntity(location));
			return resp;
		}
	}

	public HttpResponse openIDProviderReturn(String parameters, String identity)
			throws OpenRDFException, IOException, GeneralSecurityException {
		Map<String, String> map = keyByName(parameters);
		String return_to = map.get("openid.return_to");
		StringBuilder sb = new StringBuilder();
		if (return_to != null && return_to.length() > 0) {
		    String self = this.getResource().stringValue();
			StringBuilder hash = new StringBuilder(identity);
			String[] nameEmail = getNameEmail(identity);
		    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		    df.setTimeZone(TimeZone.getTimeZone("UTC"));
		    String now = df.format(new Date());
			sb.append(return_to);
			if (return_to.indexOf('?') >= 0) {
				sb.append('&');
			} else {
				sb.append('?');
			}
			sb.append("openid.ns=http://specs.openid.net/auth/2.0");
		    sb.append("&openid.mode=id_res");
		    if (nameEmail != null) {
			    sb.append("&openid.ns.ax=http://openid.net/srv/ax/1.0");
			    sb.append("&openid.ax.mode=fetch_response");
			    sb.append("&openid.ax.type.email=http://axschema.org/contact/email");
			    sb.append("&openid.ax.type.fullname=http://axschema.org/namePerson");
			    sb.append("&openid.ax.value.email=").append(encode(nameEmail[1]));
			    sb.append("&openid.ax.value.fullname=").append(encode(nameEmail[0]));
			    hash.append(':').append(nameEmail[0]).append(':').append(nameEmail[1]);
		    }
		    String response_nonce = now + hash(hash.toString(), "MD5");
		    StringBuilder sig = new StringBuilder();
			sig.append("openid.op_endpoint=").append(encode(self));
			sig.append("&openid.identity=").append(encode(identity));
			sig.append("&openid.claimed_id=").append(encode(identity));
			sig.append("&openid.return_to=").append(encode(return_to));
			sig.append("&openid.response_nonce=").append(encode(response_nonce));
			sb.append("&").append(sig);
			sb.append("&openid.signed=").append("op_endpoint,identity,claimed_id,return_to,response_nonce");
			sb.append("&openid.sig=").append(encode(sig(sig.toString())));
		} else {
			sb.append("/");
		}
		String location = sb.toString();
		BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 303, "See Other");
		resp.setHeader("Location", location);
		resp.setHeader("Cache-Control", "no-store");
		resp.setHeader("Content-Type", "text/plain");
		resp.setEntity(new StringEntity(location));
		return resp;
	
	}

	public void resetCache() throws RepositoryException {
		this.getObjectConnection().commit();
		getCalliRepository().resetCache();
	}

	public boolean isProtected(String url) throws OpenRDFException, IOException {
		return getManager().isProtected(url);
	}

	/**
	 * Called from digest.ttl
	 */
	public void registerUser(Resource invitedUser, String digestUser,
			String email, String fullname) throws OpenRDFException, IOException {
		ObjectConnection con = this.getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		registerUser(invitedUser, vf.createURI(digestUser), email, fullname, con);
		getCalliRepository().resetCache();
	}

	/**
	 * Called from realm.ttl and digest-user.ttl
	 */
	public String getUserIdentifier(String method, Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedAuthenticationManager mgr = getManager();
		if (mgr == null)
			return null;
		return mgr
				.getUserIdentifier(method, tokens, this.getObjectConnection());
	}

	/**
	 * Called from realm.ttl
	 */
	public String getUserLogin(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedAuthenticationManager mgr = getManager();
		if (mgr == null)
			return null;
		return mgr.getUserLogin(tokens, this.getObjectConnection());
	}

	/**
	 * called from realm.ttl
	 */
	public String[] getUsernameSetCookie(Collection<String> tokens)
			throws OpenRDFException, IOException {
		DetachedAuthenticationManager mgr = getManager();
		if (mgr == null)
			return null;
		return mgr.getUsernameSetCookie(tokens, this.getObjectConnection());
	}

	protected DetachedAuthenticationManager getManager()
			throws OpenRDFException, IOException {
		CalliRepository repo = getCalliRepository();
		AuthorizationManager manager = repo.getAuthorizationManager();
		return manager.getAuthenticationManager(this.getResource());
	}

	private String sig(String text) throws OpenRDFException, IOException,
			GeneralSecurityException {
		String secret = this.getRealm().getOriginSecret();
		SecretKey key = new SecretKeySpec(readBytes(secret), "HmacSHA256");
		Mac m = Mac.getInstance("HmacSHA256");
		m.init(key);
		m.update(text.getBytes("UTF-8"));
		return Base64.encodeBase64String(m.doFinal());
	}

	private byte[] readBytes(String string) {
		if (Base64.isBase64(string))
			return Base64.decodeBase64(string);
		return string.getBytes(Charset.forName("UTF-8"));
	}

	private String[] getNameEmail(String identity) throws OpenRDFException {
		ObjectConnection con = this.getObjectConnection();
		TupleQuery qry = con.prepareTupleQuery(QueryLanguage.SPARQL, IDENTITY_NAME_EMAIL);
		qry.setBinding("identity", con.getValueFactory().createURI(identity));
		TupleQueryResult results = qry.evaluate();
		try {
			if (results.hasNext()) {
				BindingSet result = results.next();
				return new String[]{result.getValue("name").stringValue(), result.getValue("email").stringValue()};
			} else {
				return null;
			}
		} finally {
			results.close();
		}
	}

	private void registerUser(Resource invitedUser, URI regUser,
			String email, String fullname, ObjectConnection con)
			throws OpenRDFException, IOException {
		ValueFactory vf = con.getValueFactory();
		RepositoryResult<Statement> stmts;
		stmts = con.getStatements((Resource) null, null, invitedUser);
		try {
			while (stmts.hasNext()) {
				moveTo(regUser, stmts.next(), con);
			}
		} finally {
			stmts.close();
		}
		stmts = con.getStatements(invitedUser, RDFS.COMMENT, null);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				add(regUser, st.getPredicate(), st.getObject(), con);
			}
		} finally {
			stmts.close();
		}
		stmts = con.getStatements(invitedUser, DCTERMS.CREATED, null);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				add(regUser, st.getPredicate(), st.getObject(), con);
			}
		} finally {
			stmts.close();
		}
		stmts = con.getStatements(invitedUser, vf.createURI(FOAF_DEPICTION), null);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				add(regUser, st.getPredicate(), st.getObject(), con);
			}
		} finally {
			stmts.close();
		}
		if (fullname == null) {
			stmts = con.getStatements(invitedUser, RDFS.LABEL, null);
			try {
				while (stmts.hasNext()) {
					Value label = stmts.next().getObject();
					if (!con.hasStatement(regUser, RDFS.LABEL, label)) {
						con.remove(regUser, RDFS.LABEL, null);
						con.add(regUser, RDFS.LABEL, label);
					}
				}
			} finally {
				stmts.close();
			}
		} else {
			Literal label = vf.createLiteral(fullname);
			if (!con.hasStatement(regUser, RDFS.LABEL, label)) {
				con.remove(regUser, RDFS.LABEL, null);
				con.add(regUser, RDFS.LABEL, label);
			}
		}
		URI hasEmail = vf.createURI(EMAIL);
		if (email == null) {
			stmts = con.getStatements(invitedUser, hasEmail, null);
			try {
				while (stmts.hasNext()) {
					Value obj = stmts.next().getObject();
					if (!con.hasStatement(regUser, hasEmail, obj)) {
						con.remove(regUser, hasEmail, null);
						con.add(regUser, hasEmail, obj);
					}
				}
			} finally {
				stmts.close();
			}
		} else {
			Literal mailto = vf.createLiteral(email);
			if (!con.hasStatement(regUser, hasEmail, mailto)) {
				con.remove(regUser, hasEmail, null);
				con.add(regUser, hasEmail, mailto);
			}
		}
		add(regUser, RDF.TYPE, vf.createURI(PARTY), con);
		add(regUser, RDF.TYPE, vf.createURI(USER), con);
		add(regUser, vf.createURI(DERIVED_FROM), invitedUser, con);
		add(regUser, DCTERMS.MODIFIED, vf.createLiteral(now()), con);
		getManager().registered(invitedUser, regUser, con);
		con.remove(invitedUser, null, null);
		con.commit();
	}

	private XMLGregorianCalendar now() {
		try {
			TimeZone utc = TimeZone.getTimeZone("UTC");
			DatatypeFactory df = DatatypeFactory.newInstance();
			return df.newXMLGregorianCalendar(new GregorianCalendar(utc));
		} catch (DatatypeConfigurationException e) {
			throw new AssertionError(e);
		}
	}

	private void moveTo(URI link, Statement st, ObjectConnection con)
			throws RepositoryException {
		URI pred = st.getPredicate();
		if (RDF.NAMESPACE.equals(pred.getNamespace()))
			return;
		if (PROV.equals(pred.getNamespace()))
			return;
		Resource subj = st.getSubject();
		con.remove(subj, pred, st.getObject());
		if (HAS_COMPONENT.equals(pred.stringValue()))
			return;
		add(subj, pred, link, con);
	}

	private void add(Resource subj, URI pred, Value obj, ObjectConnection con)
			throws RepositoryException {
		if (con.hasStatement(subj, pred, obj))
			return;
		con.add(subj, pred, obj);
	}

	private String hash(String text, String algorithm)
		throws NoSuchAlgorithmException
	{
		byte[] hash = MessageDigest.getInstance(algorithm).digest(text.getBytes());
		BigInteger bi = new BigInteger(1, hash);
		String result = bi.toString(16);
		if (result.length() % 2 != 0) {
			return "0" + result;
		}
		return result;
	}

	private Map<String, String> keyByName(String cookie) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		if (cookie != null) {
			for (String pair : cookie.split("&")) {
				int idx = pair.indexOf('=');
				if (idx > 0) {
					String name = decode(pair.substring(0, idx));
					String value = decode(pair.substring(idx + 1));
					map.put(name, value);
				}
			}
		}
		return map;
	}

}
