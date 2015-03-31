/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
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

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import javax.tools.FileObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.setup.SecretOriginProvider;
import org.callimachusproject.util.PasswordGenerator;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.InternalServerError;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates HTTP digest authorization against passwords in the RDF store.
 */
public class DigestPasswordAccessor implements DigestAccessor {
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String SELECT_PASSWORD = PREFIX
			+ "SELECT (str(?user) AS ?id) ?encoded ?passwordDigest {{\n"
			+ "?user calli:name $name .\n"
			+ "$this calli:authNamespace ?folder .\n"
			+ "?folder calli:hasComponent ?user .\n"
			+ "FILTER (str(?user) = concat(str(?folder), $name))\n"
			+ "OPTIONAL { ?user calli:encoded ?encoded; calli:algorithm \"MD5\" }\n"
			+ "OPTIONAL { ?user calli:passwordDigest ?passwordDigest }\n"
			+ "} UNION {\n" + "?user calli:email $name .\n"
			+ "$this calli:authNamespace ?folder .\n"
			+ "?folder calli:hasComponent ?user .\n"
			+ "OPTIONAL { ?user calli:passwordDigest ?passwordDigest }\n"
			+ "}} LIMIT 10";
	private static final String COPY_PERM = PREFIX
			+ "INSERT { $dst\n"
			+ "calli:reader ?reader; calli:subscriber ?subscriber; calli:contributor ?contributor; calli:editor ?editor; calli:administrator ?administrator\n"
			+ "} WHERE { { $src calli:reader ?reader }\n"
			+ "UNION { $src calli:subscriber ?subscriber }\n"
			+ "UNION { $src calli:contributor ?contributor }\n"
			+ "UNION { $src calli:editor ?editor }\n"
			+ "UNION { $src calli:administrator ?administrator }\n" + "}";
	private static final long THREE_MONTHS = 3 * 30 * 24 * 60 * 60;
	private static final BasicStatusLine _200 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 200, "OK");
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");
	private static final BasicStatusLine _401 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 401, "Unauthorized");

	private final Logger logger = LoggerFactory.getLogger(DigestPasswordAccessor.class);
	private final Resource self;
	private final RealmManager realms;
	private final String digestNonceSecure;
	private final String digestNonce;
	private final SecureRandom random = new SecureRandom();

	public DigestPasswordAccessor(Resource self, RealmManager realms) {
		assert self != null;
		assert realms != null;
		this.self = self;
		this.realms = realms;
		boolean secureOnly = self.stringValue().startsWith("https");
		this.digestNonceSecure = secureOnly ? ";Secure" : "";
		StringBuilder dn = new StringBuilder();
		dn.append("digestNonce");
		dn.append(Integer.toHexString(Math.abs(self.hashCode())));
		if (secureOnly) {
			dn.append('s');
		}
		dn.append("=");
		this.digestNonce = dn.toString();
	}

	public String toString() {
		return getIdentifier();
	}

	public String getIdentifier() {
		return self.stringValue();
	}

	public HttpResponse getNotLoggedInResponse(String method, String url,
			String[] via, Collection<String> cookies) throws AssertionError {
		HttpResponse resp = new BasicHttpResponse(_401);
		resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
		try {
			resp.setEntity(new StringEntity("Must login", "UTF-8"));
		} catch (Exception e) { // UnsupportedEncodingException
			throw new AssertionError(e);
		}
		return resp;
	}

	public HttpResponse getBadCredentialResponse(String method, String url,
			String[] via, Collection<String> cookies) throws AssertionError {
		HttpResponse resp = new BasicHttpResponse(_401);
		resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
		try {
			resp.setEntity(new StringEntity("Bad credentials", "UTF-8"));
		} catch (Exception e) { // UnsupportedEncodingException
			throw new AssertionError(e);
		}
		return resp;
	}

	public HttpResponse getLogoutResponse() {
		BasicHttpResponse resp = new BasicHttpResponse(_204);
					resp.addHeader("Set-Cookie", digestNonce
							+ ";Max-Age=0;Path=/;HttpOnly" + digestNonceSecure);
				return resp;
	}

	public HttpResponse getPersistentLogin(String iri) throws OpenRDFException,
			IOException {
		DetachedRealm realm = realms.getRealm(iri);
		if (realm == null)
			return null;
		String secret = realm.getOriginSecret();
		String nonce = nextNonce();
		BasicHttpResponse resp = new BasicHttpResponse(_200);
		resp.addHeader("Set-Cookie", digestNonce + nonce + ";Max-Age="
				+ THREE_MONTHS + ";Path=/;HttpOnly" + digestNonceSecure);
		resp.addHeader("Cache-Control", "private");
		resp.setHeader("Content-Type", "text/plain;charset=UTF-8");
		String hash = md5(nonce + ":" + secret);
		resp.setEntity(new StringEntity(hash, Charset.forName("UTF-8")));
		return resp;
	}

	public void registerUser(Resource invitedUser, URI registeredUser,
			ObjectConnection con) throws OpenRDFException {
		Update update = con.prepareUpdate(QueryLanguage.SPARQL, COPY_PERM);
		update.setBinding("src", invitedUser);
		update.setBinding("dst", registeredUser);
		update.execute();
	}

	public Map<String, String> findDigestUser(String method, String username,
			String realm, Collection<String> cookies, ObjectConnection con)
			throws OpenRDFException, IOException {
		return findDigestUser(username, realm, cookies, con);
	}

	public boolean isDigestPassword(String username, String authName, Collection<String> tokens, String[] hash,
			ObjectConnection con) throws OpenRDFException, IOException {
		Map<String, String> passwords = findDigestUser(username, authName, tokens, con);
		for (String h : hash) {
			if (passwords.containsKey(h))
				return true;
		}
		return false;
	}

	public Set<?> changeDigestPassword(Set<RDFObject> files,
			String[] passwords, String webapp, ObjectConnection con)
			throws RepositoryException, IOException {
		int i = 0;
		Set<Object> set = new LinkedHashSet<Object>();
		for (URI uuid : getPasswordFiles(files, passwords.length, webapp, con)) {
			String password = passwords[i++];
			if (password == null || password.length() == 0)
				throw new BadRequest("New password cannot be empty");
			Writer writer = con.getBlobObject(uuid).openWriter();
			try {
				writer.write(password);
			} finally {
				writer.close();
			}
			set.add(con.getObject(uuid));
		}
		return set;
	}

	public String getDaypass(String email, String secret) {
		if (secret == null)
			throw new InternalServerError("Temporary passwords are not enabled");
		long now = System.currentTimeMillis();
		return getDaypass(getHalfDay(now), email, secret);
	}

	private Map<String, String> findDigestUser(String username, String realm,
			Collection<String> cookies, ObjectConnection con)
			throws OpenRDFException, QueryEvaluationException, IOException {
		if (username == null)
			throw new NullPointerException();
		TupleQueryResult results = findPasswordDigest(username, con);
		try {
			if (!results.hasNext())
				return null;
			String nonce = getDigestNonce(cookies);
			Map<String, String> map = new HashMap<String, String>();
			while (results.hasNext()) {
				BindingSet result = results.next();
				String iri = result.getValue("id").stringValue();
				assert iri != null;
				// user (legacy) inline password
				if (result.hasBinding("encoded")) {
					map.put(encodeHex(result.getValue("encoded")), iri);
				}
				String hash = null;
				// user secret password
				if (result.hasBinding("passwordDigest")) {
					Resource value = (Resource) result.getValue("passwordDigest");
					Object file = con.getObjectFactory().createObject(value);
					hash = readString((FileObject) file);
					if (hash != null) {
						map.put(hash, iri);
					}
				}
				DetachedRealm r = realms.getRealm(iri);
				if (r == null || r.getOriginSecret() == null)
					continue;
				// remember me
				String secret = r.getOriginSecret();
				String usrlm = username + ':' + realm + ':';
				if (nonce != null && hash != null) {
					String password = md5(hash + ":" + md5(nonce + ":" + secret));
					map.put(md5(usrlm + password), iri);
				}
				// temporary password
				long now = System.currentTimeMillis();
				int d = getHalfDay(now);
				map.put(md5(usrlm + getDaypass(d, username, secret)), iri);
				map.put(md5(usrlm + getDaypass(d - 1, username, secret)), iri);
			}
			return map;
		} finally {
			results.close();
		}
	}

	private String nextNonce() {
		synchronized (random) {
			return Long.toString(Math.abs(random.nextLong()),
					Character.MAX_RADIX);
		}
	}

	private TupleQueryResult findPasswordDigest(String username,
			ObjectConnection con) throws OpenRDFException {
		TupleQuery query = con.prepareTupleQuery(SPARQL, SELECT_PASSWORD);
		query.setBinding("this", self);
		query.setBinding("name", con.getValueFactory().createLiteral(username));
		return query.evaluate();
	}

	private String getDigestNonce(Collection<String> cookies) {
		if (cookies == null)
			return null;
		for (String cookie : cookies) {
			if (cookie.contains(digestNonce)) {
				String[] pair = cookie.split("\\s*;\\s*");
				for (String p : pair) {
					if (p.startsWith(digestNonce)) {
						return p.substring(digestNonce.length());
					}
				}
			}
		}
		return null;
	}

	private Set<URI> getPasswordFiles(Set<RDFObject> files, int count,
			String webapp, ObjectConnection con) throws RepositoryException {
		if (files.size() == count) {
			Set<URI> list = new TreeSet<URI>(new ValueComparator());
			for (RDFObject file : files) {
				if (file.getResource() instanceof URI) {
					list.add((URI) file.getResource());
				}
			}
			if (list.size() == count)
				return list;
		}
		for (RDFObject file : files) {
			Resource object = file.getResource();
			if (object instanceof URI) {
				con.getBlobObject((URI) object).delete();
			}
		}
		Set<URI> list = new TreeSet<URI>(new ValueComparator());
		for (int i = 0; i < count; i++) {
			list.add(SecretOriginProvider.createSecretFile(webapp, con));
		}
		return list;
	}

	private String encodeHex(Value value) {
		Literal lit = (Literal) value;
		if (XMLSchema.HEXBINARY.equals(lit.getDatatype()))
			return lit.stringValue();
		if (XMLSchema.BASE64BINARY.equals(lit.getDatatype())) {
			byte[] bits = Base64.decodeBase64(lit.stringValue());
			return new String(Hex.encodeHex(bits));
		}
		throw new AssertionError("Unexpected datatype: " + lit);
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
			logger.error(file.toUri().toASCIIString(), e);
			return null;
		} catch (NoSuchElementException e) {
			logger.error(file.toUri().toASCIIString(), e);
			return null;
		}
	}

	private int getHalfDay(long now) {
		long halfDay = now / 1000 / 60 / 60 / 12;
		return (int) halfDay;
	}

	private String getDaypass(int day, String email, String secret) {
		if (secret == null)
			return null;
		byte[] random = readBytes(secret);
		byte[] id = email.getBytes(Charset.forName("UTF-8"));
		byte[] seed = new byte[random.length + id.length + Integer.SIZE / Byte.SIZE];
		System.arraycopy(random, 0, seed, 0, random.length);
		System.arraycopy(id, 0, seed, random.length, id.length);
		for (int i = random.length + id.length; i < seed.length; i++) {
			seed[i] = (byte) day;
			day >>= Byte.SIZE;
		}
		return new PasswordGenerator(seed).nextPassword();
	}

	private byte[] readBytes(String string) {
		if (Base64.isBase64(string))
			return Base64.decodeBase64(string);
		return string.getBytes(Charset.forName("UTF-8"));
	}

	private String md5(String text) {
		return new String(Hex.encodeHex(DigestUtils.md5(text)));
	}

}
