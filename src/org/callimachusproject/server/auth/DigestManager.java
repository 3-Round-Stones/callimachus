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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
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
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates HTTP digest authorization.
 */
public class DigestManager implements AuthenticationManager {
	private static final String DIGEST_NONCE = "DigestNonce=";
	private static final String USERNAME = "username=";
	private static final long THREE_MONTHS = 3 * 30 * 24 * 60 * 60;
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|([^,\"]*)))?\\s*,?");
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String SELECT_PASSWORD = PREFIX
			+ "SELECT (str(?user) AS ?id) ?encoded ?passwordDigest ?secret {{\n"
			+ "?user calli:name $name .\n"
			+ "$this calli:authNamespace ?folder .\n"
			+ "?folder calli:hasComponent ?user .\n"
			+ "FILTER (str(?user) = concat(str(?folder), $name))\n"
			+ "OPTIONAL { ?user calli:encoded ?encoded; calli:algorithm \"MD5\" }\n"
			+ "OPTIONAL { ?user calli:passwordDigest ?passwordDigest }\n"
			+ "OPTIONAL { ?realm calli:hasComponent* ?user; calli:secret ?secret }\n"
			+ "} UNION {\n" + "?user calli:email $name .\n"
			+ "$this calli:authNamespace ?folder .\n"
			+ "?folder calli:hasComponent ?user .\n"
			+ "OPTIONAL { ?user calli:passwordDigest ?passwordDigest }\n"
			+ "OPTIONAL { ?realm calli:hasComponent* ?user; calli:secret ?secret }\n"
			+ "}}";
	private static final BasicStatusLine _401 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 401, "Unauthorized");
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");
	private static final BasicStatusLine _200 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 200, "OK");
	private static final int MAX_NONCE_AGE = 300000; // nonce timeout of 5min
	private static final int MAX_ENTRIES = 2048;
	private static long resetAttempts;
	private static final Map<String, Integer> failedAttempts = new HashMap<String, Integer>();
	private static final Map<String, String> DIGEST_OPTS = new HashMap<String, String>();
	private static final Map<Map<String, String>, Object> replay = new LinkedHashMap<Map<String, String>, Object>() {
		private static final long serialVersionUID = -6673793531014489904L;

		protected boolean removeEldestEntry(
				Entry<Map<String, String>, Object> eldest) {
			return size() > MAX_ENTRIES;
		}
	};
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
	private final Logger logger = LoggerFactory.getLogger(DigestManager.class);
	private final Resource self;
	private final String authName;
	private final String protectedDomains;

	public DigestManager(Resource self, String authName) {
		this(self, authName, null);
	}

	public DigestManager(Resource self, String authName, String protectedDomains) {
		assert self != null;
		assert authName != null;
		this.self = self;
		this.authName = authName;
		this.protectedDomains = protectedDomains;
	}

	public String getAuthName() {
		return authName;
	}

	public String getDaypass(FileObject secret) {
		if (secret == null)
			throw new InternalServerError("Temporary passwords are not enabled");
		long now = System.currentTimeMillis();
		return getDaypass(getHalfDay(now), readString(secret));
	}

	@Override
	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) {
		Object realm = getAuthName();
		if (realm == null)
			return null;
		String domain = protectionDomain();
		if (domain == null) {
			domain = "";
		} else if (domain.length() != 0) {
			domain = ", domain=\"" + domain + "\"";
		}
		String nonce = nextNonce(resource, request.get("via"));
		String authenticate = "Digest realm=\"" + realm + "\"" + domain
				+ ", nonce=\"" + nonce
				+ "\", algorithm=\"MD5\", qop=\"auth\"";
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
		String ha2 = md5(":" + uri);
		String rspauth = md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce
				+ ":auth:" + ha2);
		BasicHttpResponse resp = new BasicHttpResponse(_204);
		String authenticate = "qop=auth,cnonce=\"" + cnonce + "\",nc=" + nc
				+ ",rspauth=\"" + rspauth + "\"";
		resp.addHeader("Authentication-Info", authenticate);
		resp.addHeader("Set-Cookie", USERNAME + encode(username));
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
					failedAttempt(options.get("username"));
				}
				return null;
			}
			if (options.containsKey("qop")) {
				boolean replayed;
				synchronized (replay) {
					replayed = replay.put(options, Boolean.TRUE) != null;
				}
				if (replayed) {
					logger.info("Request replayed {}", options);
					failedAttempt(options.get("username"));
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
	public String findCredential(Collection<String> tokens, ObjectConnection con)
			throws OpenRDFException {
		Map<String, String> options = parseDigestAuthorization(tokens);
		if (options == null)
			return null;
		String username = options.get("username");
		if (username == null)
			throw new BadRequest("Missing username");
		String realm = options.get("realm");
		Map<String, String> passwords = findDigestUser(username, realm, tokens,
				con);
		if (passwords == null || passwords.isEmpty())
			return null;
		return passwords.values().iterator().next();
	}

	@Override
	public String findCredentialLabel(Collection<String> tokens, ObjectConnection con) {
		Map<String, String> options = parseDigestAuthorization(tokens);
		if (options == null)
			return null;
		String username = options.get("username");
		if (username == null)
			throw new BadRequest("Missing username");
		return username;
	}

	public HttpResponse login(Collection<String> tokens, boolean persistent,
			ObjectConnection con) throws OpenRDFException {
		if (persistent) {
			String username = findCredentialLabel(tokens, con);
			TupleQueryResult results = findPasswordDigest(username, con);
			try {
				while (results.hasNext()) {
					Value secret = results.next().getValue("secret");
					if (secret instanceof Resource) {
						ObjectFactory of = con.getObjectFactory();
						RDFObject file = of.createObject((Resource) secret);
						return login((FileObject) file);
					}
				}
			} finally {
				results.close();
			}
		}
		return new BasicHttpResponse(_204);
	}

	public HttpMessage logout() {
		BasicHttpResponse resp = new BasicHttpResponse(_204);
		resp.addHeader("Set-Cookie", DIGEST_NONCE
				+ ";Max-Age=0;Path=/;HttpOnly");
		resp.addHeader("Set-Cookie", USERNAME + ";Max-Age=0;Path=/");
		return resp;
	}

	public boolean isDigestPassword(Collection<String> tokens, String[] hash,
			ObjectConnection con) throws OpenRDFException {
		Map<String, String> auth = parseDigestAuthorization(tokens);
		String username = auth.get("username");
		String realm = auth.get("realm");
		Map<String, String> passwords = findDigestUser(username, realm, tokens,
				con);
		for (String h : hash) {
			if (passwords.containsKey(h))
				return true;
		}
		return false;
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

	private String protectionDomain() {
		return protectedDomains;
	}

	private TupleQueryResult findPasswordDigest(String username,
			ObjectConnection con) throws OpenRDFException {
		TupleQuery query = con.prepareTupleQuery(SPARQL, SELECT_PASSWORD);
		query.setBinding("this", self);
		return query.evaluate();
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
		String ha2 = md5(method + ":" + uri);
		assert username != null;
		Map<String,String> passwords = findDigestUser(username, realm, asList(request.get("cookie")), con);
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

	/**
	 * 
	 * @param username
	 *            must not be null
	 * @return Map of HEX encoded MD5 to user IRI
	 * @throws QueryEvaluationException
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 */
	private Map<String, String> findDigestUser(String username, String realm,
			Collection<String> cookies, ObjectConnection con)
			throws OpenRDFException {
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
				if (result.hasBinding("secret")) {
					Resource value = (Resource) result.getValue("secret");
					Object file = con.getObjectFactory().createObject(value);
					String secret = readString((FileObject) file);
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
			}
			return map;
		} finally {
			results.close();
		}
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

	private HttpResponse login(FileObject file) {
		String nonce = Long.toString(Math.abs(new SecureRandom().nextLong()),
				Character.MAX_RADIX);
		BasicHttpResponse resp = new BasicHttpResponse(_200);
		resp.addHeader("Set-Cookie", DIGEST_NONCE + nonce + ";Max-Age="
				+ THREE_MONTHS + ";Path=/;HttpOnly");
		resp.setHeader("Content-Type", "text/plain;charset=UTF-8");
		String hash = md5(nonce + ":" + readString(file));
		resp.setEntity(new StringEntity(hash, Charset.forName("UTF-8")));
		return resp;
	}

	private void failedAttempt(String username) {
		synchronized (failedAttempts) {
			long now = System.currentTimeMillis();
			if (resetAttempts < now) {
				failedAttempts.clear();
				resetAttempts = now + 60 * 60 * 1000;
			}
			try {
				if (username == null || "logout".equals(username)) {
					Thread.sleep(1000);
				} else {
					Integer count = failedAttempts.get(username);
					if (count == null) {
						failedAttempts.put(username, 1);
						Thread.sleep(1000);
					} else if (count > 100) {
						failedAttempts.put(username, count + 1);
						Thread.sleep(10000);
					} else {
						failedAttempts.put(username, count + 1);
						Thread.sleep(1000);
					}
				}
			} catch (InterruptedException e) {
				// continue
			}
		}
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
		String realmAuth = getAuthName();
		if (realm == null || !realm.equals(realmAuth)
				|| !url.equals(uri) && !path.equals(uri)) {
			logger.info("Bad authorization on {} using {}", url, authorization);
			throw new BadRequest("Bad Authorization");
		}
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

	private String hash(String[] via) {
		long code = 0;
		if (via != null) {
			for (String str : via) {
				code = code * 31 + str.hashCode();
			}
		}
		return Long.toString(code, Character.MAX_RADIX);
	}

	private String md5(String text) {
		return new String(Hex.encodeHex(DigestUtils.md5(text)));
	}

	private String encode(String username) {
		try {
			return URLEncoder.encode(username, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

}
