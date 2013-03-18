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
import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.FileObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
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
import org.callimachusproject.setup.SecretRealmProvider;
import org.callimachusproject.traits.VersionedObject;
import org.callimachusproject.util.PasswordGenerator;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
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
public class DetachedDigestManager implements DetachedAuthenticationManager {
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
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|([^,\"]*)))?\\s*,?");
	private static final long THREE_MONTHS = 3 * 30 * 24 * 60 * 60;
	private static final int MAX_NONCE_AGE = 300000; // nonce timeout of 5min
	private static final BasicStatusLine _403 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 403, "Forbidden");
	private static final BasicStatusLine _401 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 401, "Unauthorized");
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");
	private static final BasicStatusLine _200 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 200, "OK");
	private static final Map<String, String> DIGEST_OPTS = new HashMap<String, String>();
	static {
		DIGEST_OPTS.put("realm", null);
		DIGEST_OPTS.put("nonce", null);
		DIGEST_OPTS.put("username", null);
		DIGEST_OPTS.put("uri", null);
		DIGEST_OPTS.put("qop", null);
		DIGEST_OPTS.put("cnonce", null);
		DIGEST_OPTS.put("nc", null);
		DIGEST_OPTS.put("response", null);
	}
	private final Logger logger = LoggerFactory.getLogger(DetachedDigestManager.class);
	private final Resource self;
	private final String authName;
	private final String protectedDomains;
	private final String protectedPath;
	private final RealmManager realms;
	private final FailManager fail = new FailManager();
	private final String digestNonceSecure;
	private final String digestNonce;
	private final Set<String> userCookies = new LinkedHashSet<String>();

	public DetachedDigestManager(Resource self, String authName, String path, List<String> domains, RealmManager realms) {
		assert self != null;
		assert authName != null;
		assert realms != null;
		this.realms = realms;
		this.self = self;
		this.authName = authName;
		this.protectedPath = path;
		assert domains != null;
		assert domains.size() > 0;
		boolean secureOnly = true;
		Set<Integer> ports = new HashSet<Integer>();
		StringBuilder sb = new StringBuilder();
		for (String domain : domains) {
			sb.append(' ').append(domain);
			int port = java.net.URI.create(domain).getPort();
			ports.add(port);
			StringBuilder suffix = new StringBuilder();
			if (port > 0) {
				suffix.append(port);
			}
			if (domain.startsWith("https")) {
				suffix.append('s');
			} else {
				secureOnly = false;
			}
			suffix.append('=');
			userCookies.add("username" + suffix);
		}
		this.protectedDomains = sb.substring(1);
		this.digestNonceSecure = secureOnly ? ";Secure" : "";
		StringBuilder dn = new StringBuilder();
		dn.append("digestNonce");
		dn.append(Integer.toHexString(Math.abs(self.hashCode())));
		if (ports.size() == 1) {
			Integer port = ports.iterator().next();
			if (port > 0) {
				dn.append(port);
			}
		}
		if (secureOnly) {
			dn.append('s');
		}
		dn.append("=");
		this.digestNonce = dn.toString();
	}

	@Override
	public String toString() {
		return getIdentifier();
	}

	@Override
	public String getIdentifier() {
		return self.stringValue();
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
			throws OpenRDFException, IOException {
		Map<String, String> auth = parseDigestAuthorization(request);
		if (auth == null)
			return null;
		Map.Entry<String, String> password = findAuthUser(method, resource,
				request, auth, con);
		if (password == null)
			return null;
		String cnonce = auth.get("cnonce");
		String nc = auth.get("nc");
		String uri = auth.get("uri");
		String nonce = auth.get("nonce");
		String ha1 = password.getKey();
		String ha2 = md5(":" + uri);
		String rspauth = md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce
				+ ":auth:" + ha2);
		String authenticate = "qop=auth,cnonce=\"" + cnonce + "\",nc=" + nc
				+ ",rspauth=\"" + rspauth + "\"";
		BasicHttpResponse resp = new BasicHttpResponse(_204);
		resp.addHeader("Authentication-Info", authenticate);
		return resp;
	}

	@Override
	public String authenticateRequest(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con) {
		try {
			Map<String, String> options = parseDigestAuthorization(request);
			if (options == null)
				return null;
			Map.Entry<String, String> password = findAuthUser(method, resource,
					request, options, con);
			String username = options.get("username");
			if (password == null) {
				if (isRecentDigest(resource, request, options)) {
					fail.failedAttempt(username);
				}
				return null;
			}
			if (options.containsKey("qop")) {
				if (fail.isReplayed(options)) {
					fail.failedAttempt(username);
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
			if (token.indexOf("username=\"-\"") > 0) {
				// # bogus credentials received
				BasicHttpResponse resp = new BasicHttpResponse(_204);
					resp.addHeader("Set-Cookie", digestNonce
							+ ";Max-Age=0;Path=/;HttpOnly" + digestNonceSecure);
				for (String userCookie : userCookies) {
					String secure = userCookie.endsWith("s=") ? ";Secure" : "";
					resp.addHeader("Set-Cookie", userCookie
							+ ";Max-Age=0;Path=" + protectedPath + secure);
				}
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
			ObjectConnection con) throws OpenRDFException, IOException {
		HttpResponse resp = null;
		String username = getUserLogin(tokens, con);
		if (persistent) {
			resp = getPersistentLogin(username, con);
		}
		if (resp == null) {
			resp = new BasicHttpResponse(_204);
		}
		String[] cookies = getUsernameSetCookie(tokens, con);
		if (cookies.length == 0)
			return new BasicHttpResponse(_403);
		for (String cookie : cookies) {
			resp.addHeader("Set-Cookie", cookie);
		}
		return resp;
	}

	public String[] getUsernameSetCookie(Collection<String> tokens,
			ObjectConnection con) {
		String username = getUserLogin(tokens, con);
		if (username == null)
			return new String[0];
		int i = 0;
		String[] result = new String[userCookies.size()];
		for (String userCookie : userCookies) {
			String secure = userCookie.endsWith("s=") ? ";Secure" : "";
			result[i++] = userCookie + encode(username) + ";Path=" + protectedPath + secure;
		}
		return result;
	}

	public String getUserIdentifier(Collection<String> tokens,
			ObjectConnection con) throws OpenRDFException, IOException {
		Map<String, String> options = parseDigestAuthorization(tokens);
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

	public String getUserLogin(Collection<String> tokens, ObjectConnection con) {
		Map<String, String> options = parseDigestAuthorization(tokens);
		if (options == null)
			return null;
		String username = options.get("username");
		if (username == null)
			throw new BadRequest("Missing username");
		return username;
	}

	public boolean isDigestPassword(Collection<String> tokens, String[] hash,
			ObjectConnection con) throws OpenRDFException, IOException {
		Map<String, String> auth = parseDigestAuthorization(tokens);
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

	public Set<?> changeDigestPassword(Set<RDFObject> files,
			String[] passwords, String webapp, ObjectConnection con)
			throws RepositoryException, IOException {
		int i = 0;
		Set<Object> set = new LinkedHashSet<Object>();
		for (URI uuid : getPasswordFiles(files, passwords.length, webapp, con)) {
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
			throws OpenRDFException, IOException {
		if (!isRecentDigest(resource, request, auth))
			return null;
		String qop = auth.get("qop");
		String uri = auth.get("uri");
		String nonce = auth.get("nonce");
		String username = auth.get("username");
		String realm = auth.get("realm");
		String response = auth.get("response");
		String ha2 = md5(method + ":" + uri);
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
			if (qop == null && md5(legacy).equals(response))
				return e;
			String expected = ha1 + ":" + nonce + ":" + auth.get("nc") + ":"
					+ auth.get("cnonce") + ":" + qop + ":" + ha2;
			if (md5(expected).equals(response))
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
			ObjectConnection con) throws OpenRDFException, IOException {
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
					if (hash != null) {
						map.put(hash, iri);
					}
				}
				DetachedRealm r = realms.getRealm(iri);
				if (r == null || r.getSecret() == null)
					continue;
				String secret = r.getSecret();
				if (nonce != null && hash != null) {
					String password = md5(hash + ":" + md5(nonce + ":" + secret));
					map.put(md5(username + ':' + realm + ':' + password), iri);
				}
				long now = System.currentTimeMillis();
				short halfDay = getHalfDay(now);
				for (short d = halfDay; d >= halfDay - 1; d--) {
					String daypass = getDaypass(d, secret);
					map.put(md5(username + ':' + realm + ':' + daypass), iri);
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

	private HttpResponse getPersistentLogin(String username,
			ObjectConnection con) throws OpenRDFException, IOException {
		TupleQueryResult results = findPasswordDigest(username, con);
		try {
			while (results.hasNext()) {
				String iri = results.next().getValue("id").stringValue();
				DetachedRealm realm = realms.getRealm(iri);
				if (realm != null) {
					String secret = realm.getSecret();
					return getPersistentLogin(secret);
				}
			}
		} finally {
			results.close();
		}
		return null;
	}

	private HttpResponse getPersistentLogin(String secret) {
		String nonce = Long.toString(Math.abs(new SecureRandom().nextLong()),
				Character.MAX_RADIX);
		BasicHttpResponse resp = new BasicHttpResponse(_200);
		resp.addHeader("Set-Cookie", digestNonce + nonce + ";Max-Age="
				+ THREE_MONTHS + ";Path=/;HttpOnly" + digestNonceSecure);
		resp.addHeader("Cache-Control", "private");
		resp.setHeader("Content-Type", "text/plain;charset=UTF-8");
		String hash = md5(nonce + ":" + secret);
		resp.setEntity(new StringEntity(hash, Charset.forName("UTF-8")));
		return resp;
	}

	private String nextNonce(Object resource, String[] via) {
		String ip = hash(via);
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
		return parseDigestAuthorization(asList(authorization));
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
			if (!hash(via).equals(nonce.substring(last + 1)))
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
		if (secret == null)
			return null;
		byte[] random = readBytes(secret);
		byte[] seed = new byte[random.length + Short.SIZE / Byte.SIZE];
		System.arraycopy(random, 0, seed, 0, random.length);
		for (int i = random.length; i < seed.length; i++) {
			seed[i] = (byte) day;
			day >>= Byte.SIZE;
		}
		return new PasswordGenerator(seed).nextPassword();
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
			list.add(SecretRealmProvider.createSecretFile(webapp, con));
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

	private Map<String, String> parseDigestAuthorization(
			Collection<String> authorization) {
		for (String digest : authorization) {
			if (digest == null || !digest.startsWith("Digest "))
				continue;
			String options = digest.substring("Digest ".length());
			Map<String, String> result = new HashMap<String, String>(
					DIGEST_OPTS);
			Matcher m = TOKENS_REGEX.matcher(options);
			while (m.find()) {
				String key = m.group(1);
				if (result.containsKey(key)) {
					if (m.group(2) != null) {
						result.put(key, m.group(2));
					} else if (m.group(3) != null) {
						result.put(key, m.group(3));
					}
				}
			}
			return result;
		}
		return null;
	}

	private String md5(String text) {
		return new String(Hex.encodeHex(DigestUtils.md5(text)));
	}

	private String hash(String... values) {
		long code = 0;
		if (values != null) {
			for (String str : values) {
				code = code * 31 + str.hashCode();
			}
		}
		return Long.toString(code, Character.MAX_RADIX);
	}

	private String encode(String username) {
		try {
			return URLEncoder.encode(username, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

}
