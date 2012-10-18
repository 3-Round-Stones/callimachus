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
package org.callimachusproject.server.auth;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.tools.FileObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.traits.VersionedObject;
import org.callimachusproject.util.PasswordGenerator;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates HTTP digest authorization.
 */
public class DigestManager implements AuthenticationManager {
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
			+ "}}";
	private static final String DIGEST_NONCE = "digestNonce=";
	private static final long THREE_MONTHS = 3 * 30 * 24 * 60 * 60;
	private static final int MAX_NONCE_AGE = 300000; // nonce timeout of 5min
	private static final String USERNAME = "username=";
	private static final BasicStatusLine _401 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 401, "Unauthorized");
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");
	private static final BasicStatusLine _200 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 200, "OK");
	private final Logger logger = LoggerFactory.getLogger(DigestManager.class);
	private final Resource self;
	private final String authName;
	private final String protectedDomains;
	private final String protectedPath;
	private final RealmManager realms;
	private final DigestHelper helper = new DigestHelper();

	DigestManager(Resource self, String authName, String protectedDomains, RealmManager realms) {
		assert self != null;
		assert authName != null;
		assert protectedDomains != null;
		assert protectedDomains.length() > 0;
		assert realms != null;
		this.realms = realms;
		this.self = self;
		this.authName = authName;
		this.protectedDomains = protectedDomains;
		if (protectedDomains.contains(" ")) {
			String pre = null;
			for (String url : protectedDomains.split("\\s+")) {
				if (url.length() > 0) {
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
			}
			protectedPath = pre == null ? "/" : pre;
		} else {
			protectedPath = new ParsedURI(protectedDomains).getPath();
		}
	}

	@Override
	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) {
		String nonce = nextNonce(resource, request.get("via"));
		String authenticate = "Digest realm=\"" + authName + "\""
				+ (", domain=\"" + protectedDomains + "\"") + ", nonce=\""
				+ nonce + "\", algorithm=\"MD5\", qop=\"auth\"";
		Map<String, String> options = parseDigestAuthorization(request);
		if (options != null) {
			if (!isRecentDigest(resource, request, options)) {
				authenticate += ",stale=true";
				HttpResponse resp = new BasicHttpResponse(_401);
				resp.setHeader("Cache-Control", "no-store");
				resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
				resp.setHeader("WWW-Authenticate", authenticate);
				try {
					resp.setEntity(new StringEntity("Stale authorization header", "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new AssertionError(e);
				}
				return resp;
			}
		}
		HttpResponse resp = new BasicHttpResponse(_401);
		resp.setHeader("Cache-Control", "no-store");
		resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
		resp.setHeader("WWW-Authenticate", authenticate);
		try {
			resp.setEntity(new StringEntity("Unauthorized", "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
		return resp;
	}

	@Override
	public HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con)
			throws OpenRDFException {
		Map<String, String> auth = parseDigestAuthorization(request);
		Map.Entry<String, String> password = findAuthUser(method, resource,
				request, auth, con);
		if (password == null)
			return null;
		String username = auth.get("username");
		String cnonce = auth.get("cnonce");
		String nc = auth.get("nc");
		String uri = auth.get("uri");
		String nonce = auth.get("nonce");
		String ha1 = password.getKey();
		String ha2 = helper.md5(":" + uri);
		String rspauth = helper.md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce
				+ ":auth:" + ha2);
		String authenticate = "qop=auth,cnonce=\"" + cnonce + "\",nc=" + nc
				+ ",rspauth=\"" + rspauth + "\"";
		String cookie = USERNAME + helper.encode(username) + ";Path=" + protectedPath;
		BasicHttpResponse resp = new BasicHttpResponse(_204);
		resp.addHeader("Authentication-Info", authenticate);
		resp.addHeader("Set-Cookie", cookie);
		return resp;
	}

	@Override
	public String authenticateRequest(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con) {
		try {
			Map<String, String> options = parseDigestAuthorization(request);
			Map.Entry<String, String> password = findAuthUser(method, resource, request, options, con);
			if (password == null) {
				if (isRecentDigest(resource, request, options)) {
					helper.failedAttempt(options.get("username"));
				}
				return null;
			}
			if (options.containsKey("qop")) {
				if (helper.isReplayed(options)) {
					logger.info("Request replayed {}", options);
					return null;
				}
			}
			return password.getValue();
		} catch (BadRequest e) {
			throw e;
		} catch (Exception e) {
			logger.warn(e.toString(), e);
			return null;
		}
	}

	@Override
	public HttpResponse logout(Collection<String> tokens) {
		for (String token : tokens) {
			if (token.indexOf("username=\"logout\"") > 0) {
				// # bogus credentials received
				BasicHttpResponse resp = new BasicHttpResponse(_204);
				resp.addHeader("Set-Cookie", DIGEST_NONCE + ";Max-Age=0;Path=/;HttpOnly");
				resp.addHeader("Set-Cookie", USERNAME + ";Max-Age=0;Path="
						+ protectedPath);
				return resp;
			}
		}
		// # the browser must send invalid credentials to logout
		BasicHttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1,
				401, "Unauthorized");
		String hd = "Digest realm=\"" + authName + "\", domain=\""
				+ protectedDomains
				+ "\", nonce=\"logout\", algorithm=\"MD5\", qop=\"auth\"";
		resp.setHeader("WWW-Authenticate", hd);
		return resp;
	}

	public HttpResponse login(Collection<String> tokens, boolean persistent,
			ObjectConnection con) throws OpenRDFException {
		if (persistent) {
			String username = findCredentialLabel(tokens, con);
			TupleQueryResult results = findPasswordDigest(username, con);
			try {
				while (results.hasNext()) {
					String iri = results.next().getValue("id").stringValue();
					Realm realm = realms.getRealm(iri, con.getRepository());
					if (realm != null) {
						String secret = realm.getSecret();
						return login(secret);
					}
				}
			} finally {
				results.close();
			}
		}
		return new BasicHttpResponse(_204);
	}

	public String findCredential(Collection<String> tokens,
			ObjectConnection con) throws OpenRDFException {
		Map<String, String> options = helper.parseDigestAuthorization(tokens);
		if (options == null)
			return null;
		String username = options.get("username");
		if (username == null)
			throw new BadRequest("Missing username");
		String realm = options.get("realm");
		Map<String, String> passwords = findDigestUser(username, realm, tokens, con);
		if (passwords == null || passwords.isEmpty())
			return null;
		return passwords.values().iterator().next();
	}

	public String findCredentialLabel(Collection<String> tokens, ObjectConnection con) {
		Map<String, String> options = helper.parseDigestAuthorization(tokens);
		if (options == null)
			return null;
		String username = options.get("username");
		if (username == null)
			throw new BadRequest("Missing username");
		return username;
	}

	public boolean isDigestPassword(Collection<String> tokens, String[] hash,
			ObjectConnection con) throws OpenRDFException {
		Map<String, String> auth = helper.parseDigestAuthorization(tokens);
		String username = auth.get("username");
		String realm = auth.get("realm");
		Map<String, String> passwords = findDigestUser(username, realm, tokens, con);
		for (String h : hash) {
			if (passwords.containsKey(h))
				return true;
		}
		return false;
	}

	public String getDaypass(FileObject secret) {
		if (secret == null)
			throw new InternalServerError("Temporary passwords are not enabled");
		long now = System.currentTimeMillis();
		return getDaypass(getHalfDay(now), readString(secret));
	}

	public Set<?> changeDigestPassword(Set<RDFObject> files, String[] passwords, ObjectConnection con)
			throws RepositoryException, IOException {
		int i = 0;
		Set<Object> set = new LinkedHashSet<Object>();
		for (URI uuid : getPasswordFiles(files, passwords.length, con)) {
			Writer writer = con.getBlobObject(uuid).openWriter();
			try {
				writer.write(passwords[i++]);
			} finally {
				writer.close();
			}
			set.add(con.getObject(uuid));
		}
		return set;
	}

	private Map.Entry<String, String> findAuthUser(String method,
			Object resource, Map<String, String[]> request,
			Map<String, String> auth, ObjectConnection con)
			throws OpenRDFException {
		if (!isRecentDigest(resource, request, auth))
			return null;
		String qop = auth.get("qop");
		String uri = auth.get("uri");
		String nonce = auth.get("nonce");
		String username = auth.get("username");
		String realm = auth.get("realm");
		String response = auth.get("response");
		String ha2 = helper.md5(method + ":" + uri);
		assert username != null;
		Map<String, String> passwords = findDigestUser(username, realm,
				asList(request.get("cookie")), con);
		if (passwords == null) {
			logger.info("Account not found: {}", username);
			return null;
		}
		for (Map.Entry<String, String> e : passwords.entrySet()) {
			String ha1 = e.getKey();
			String legacy = ha1 + ":" + nonce + ":" + ha2;
			if (qop == null && helper.md5(legacy).equals(response))
				return e;
			String expected = ha1 + ":" + nonce + ":" + auth.get("nc") + ":"
					+ auth.get("cnonce") + ":" + qop + ":" + ha2;
			if (helper.md5(expected).equals(response))
				return e;
		}
		if (passwords.isEmpty()) {
			logger.info("Missing password for: {}", username);
			return null;
		} else {
			logger.info("Passwords don't match for: {}", username);
			return null;
		}
	}

	private Map<String, String> findDigestUser(String username, String realm,
			Collection<String> cookies,
			ObjectConnection con) throws OpenRDFException {
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
				if (result.hasBinding("encoded")) {
					map.put(encodeHex(result.getValue("encoded")), iri);
				}
				String hash = null;
				if (result.hasBinding("passwordDigest")) {
					Resource value = (Resource) result.getValue("passwordDigest");
					Object file = con.getObjectFactory().createObject(value);
					hash = readString((FileObject) file);
					map.put(hash, iri);
				}
				Realm r = realms.getRealm(iri, con.getRepository());
				if (r == null || r.getSecret() == null)
					continue;
				String secret = r.getSecret();
				if (nonce != null && hash != null) {
					String password = helper.md5(hash + ":" + helper.md5(nonce + ":" + secret));
					map.put(helper.md5(username + ':' + realm + ':' + password), iri);
				}
				long now = System.currentTimeMillis();
				short halfDay = getHalfDay(now);
				for (short d = halfDay; d >= halfDay - 1; d--) {
					String daypass = getDaypass(d, secret);
					map.put(helper.md5(username + ':' + realm + ':' + daypass), iri);
				}
			}
			return map;
		} finally {
			results.close();
		}
	}

	private TupleQueryResult findPasswordDigest(String username,
			ObjectConnection con) throws OpenRDFException {
		TupleQuery query = con.prepareTupleQuery(SPARQL, SELECT_PASSWORD);
		query.setBinding("this", self);
		query.setBinding("name", con.getValueFactory().createLiteral(username));
		return query.evaluate();
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

	private String getDigestNonce(Collection<String> cookies) {
		if (cookies == null)
			return null;
		for (String cookie : cookies) {
			if (!cookie.contains(DIGEST_NONCE))
				continue;
			String[] pair = cookie.split("\\s*;\\s*");
			for (String p : pair) {
				if (p.startsWith(DIGEST_NONCE)) {
					return p.substring(DIGEST_NONCE.length());
				}
			}
		}
		return null;
	}

	private HttpResponse login(String secret) {
		String nonce = Long.toString(Math.abs(new SecureRandom().nextLong()),
				Character.MAX_RADIX);
		BasicHttpResponse resp = new BasicHttpResponse(_200);
		resp.addHeader("Set-Cookie", DIGEST_NONCE + nonce + ";Max-Age="
				+ THREE_MONTHS + ";Path=/;HttpOnly");
		resp.setHeader("Content-Type", "text/plain;charset=UTF-8");
		String hash = helper.md5(nonce + ":" + secret);
		resp.setEntity(new StringEntity(hash, Charset.forName("UTF-8")));
		return resp;
	}

	private String nextNonce(Object resource, String[] via) {
		String ip = helper.hash(via);
		String revision = getRevisionOf(resource);
		long now = System.currentTimeMillis();
		String time = Long.toString(now, Character.MAX_RADIX);
		return time + ":" + revision + ":" + ip;
	}

	private String getRevisionOf(Object resource) {
		if (resource instanceof VersionedObject) {
			String revision = ((VersionedObject) resource).revision();
			if (revision != null)
				return revision;
		}
		return "";
	}

	private Map<String, String> parseDigestAuthorization(
			Map<String, String[]> request) {
		String[] authorization = request.get("authorization");
		if (authorization == null)
			return null;
		return helper.parseDigestAuthorization(asList(authorization));
	}

	private Collection<String> asList(String[] array) {
		if (array == null)
			return null;
		return Arrays.asList(array);
	}

	private boolean isRecentDigest(Object target,
			Map<String, String[]> request, Map<String, String> authorization) {
		if (authorization == null)
			return false;
		String url = request.get("request-target")[0];
		String date = request.get("date")[0];
		String[] via = request.get("via");
		String realm = authorization.get("realm");
		String uri = authorization.get("uri");
		String username = authorization.get("username");
		if (username == null)
			throw new BadRequest("Missing username");
		ParsedURI parsed = new ParsedURI(url);
		String path = parsed.getPath();
		if (parsed.getQuery() != null) {
			path = path + "?" + parsed.getQuery();
		}
		if (realm == null || !url.equals(uri) && !path.equals(uri)) {
			logger.info("Bad authorization on {} using {}", url, authorization);
			throw new BadRequest("Bad Authorization");
		}
		if (!realm.equals(authName))
			return false;
		try {
			long now = DateUtil.parseDate(date).getTime();
			String nonce = authorization.get("nonce");
			if (nonce == null)
				return false;
			int first = nonce.indexOf(':');
			int last = nonce.lastIndexOf(':');
			if (first < 0 || last < 0)
				return false;
			if (!helper.hash(via).equals(nonce.substring(last + 1)))
				return false;
			String revision = nonce.substring(first + 1, last);
			if (!revision.equals(getRevisionOf(target)))
				return false;
			String time = nonce.substring(0, first);
			Long ms = Long.valueOf(time, Character.MAX_RADIX);
			long age = now - ms;
			return age < MAX_NONCE_AGE;
		} catch (NumberFormatException e) {
			logger.debug(e.toString(), e);
			return false;
		} catch (DateParseException e) {
			logger.warn(e.toString(), e);
			return false;
		}
	}

	private short getHalfDay(long now) {
		long halfDay = now / 1000 / 60 / 60 / 12;
		return (short) halfDay;
	}

	private String getDaypass(short day, String secret) {
		byte[] random = readBytes(secret);
		byte[] seed = new byte[random.length + Short.SIZE / Byte.SIZE];
		System.arraycopy(random, 0, seed, 0, random.length);
		for (int i = random.length; i < seed.length; i++) {
			seed[i] = (byte) day;
			day >>= Byte.SIZE;
		}
		return new PasswordGenerator(seed).nextPassword();
	}

	private Set<URI> getPasswordFiles(Set<RDFObject> files, int count, ObjectConnection con)
			throws RepositoryException {
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
		ValueFactory vf = con.getValueFactory();
		for (RDFObject file : files) {
			Resource object = file.getResource();
			if (object instanceof URI) {
				con.getBlobObject((URI) object).delete();
			}
		}
		Set<URI> list = new TreeSet<URI>(new ValueComparator());
		for (int i = 0; i < count; i++) {
			URI uuid = vf.createURI("urn:uuid:" + UUID.randomUUID());
			list.add(uuid);
		}
		return list;
	}

	private byte[] readBytes(String string) {
		if (Base64.isBase64(string))
			return Base64.decodeBase64(string);
		return string.getBytes(Charset.forName("UTF-8"));
	}

	private String readString(FileObject file) {
		try {
			Reader reader = file.openReader(true);
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

}
