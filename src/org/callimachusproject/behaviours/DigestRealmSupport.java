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
package org.callimachusproject.behaviours;

import info.aduna.net.ParsedURI;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.concepts.DigestRealm;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.name;
import org.openrdf.repository.object.annotations.sparql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates HTTP digest authorization.
 */
public abstract class DigestRealmSupport extends RealmSupport implements
		DigestRealm, RDFObject {
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"((?:[^\"]|\"\")*)\"|([^,\"]*)))?\\s*,?");
	private static final String PREFIX = "PREFIX :<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final BasicStatusLine _401 = new BasicStatusLine(
			new ProtocolVersion("HTTP", 1, 1), 401, "Unauthorized");
	private static final BasicStatusLine _204 = new BasicStatusLine(
			new ProtocolVersion("HTTP", 1, 1), 401, "No Content");
	private static final int MAX_NONCE_AGE = 15000;
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
	private Logger logger = LoggerFactory.getLogger(DigestRealmSupport.class);

	@Override
	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		Object realm = getAuthName();
		if (realm == null)
			return forbidden(method, resource, request);
		String domain = protectionDomain();
		if (domain == null) {
			domain = "";
		} else if (domain.length() != 0) {
			domain = ", domain=\"" + domain + "\"";
		}
		String nonce = nextNonce(resource, request.get("via"));
		String authenticate = "Digest realm=\"" + realm + "\"" + domain
				+ ", nonce=\"" + nonce
				+ "\", algorithm=\"MD5\", qop=\"auth,auth-int\"";
		String[] auth = request.get("authorization");
		if (auth != null && auth.length == 1 && auth[0] != null
				&& auth[0].startsWith("Digest")) {
			String string = auth[0].substring("Digest ".length());
			Map<String, String> options = parseOptions(string);
			if (!verify(options.get("nonce"), resource, request.get("via"))) {
				authenticate += ",stale=true";
				HttpResponse resp = new BasicHttpResponse(_401);
				resp.setHeader("Cache-Control", "no-store");
				resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
				resp.setEntity(new StringEntity("Stale authorization header", "UTF-8"));
				resp.setHeader("WWW-Authenticate", authenticate);
				return resp;
			}
		}
		HttpResponse resp = super.unauthorized(method, resource, request);
		if (resp == null) {
			resp = new BasicHttpResponse(_401);
			resp.setHeader("Cache-Control", "no-store");
			resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
			resp.setEntity(new StringEntity("Unauthorized", "UTF-8"));
		}
		resp.setHeader("WWW-Authenticate", authenticate);
		return resp;
	}

	@Override
	public HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request) {
		HttpMessage resp = super.authenticationInfo(method, resource, request);
		String[] auth = request.get("authorization");
		if (auth == null || auth.length != 1 || auth[0] == null
				|| !auth[0].startsWith("Digest"))
			return resp;
		String string = auth[0].substring("Digest ".length());
		Map<String, String> options = parseOptions(string);
		String cnonce = options.get("cnonce");
		String nc = options.get("nc");
		String uri = options.get("uri");
		String nonce = options.get("nonce");
		String username = options.get("username");
		String ha2 = md5(":" + uri);
		List<Object[]> encodings = findDigest(username);
		for (Object[] row : encodings) {
			byte[] a1 = (byte[]) row[1];
			if (a1 == null)
				continue;
			String ha1 = new String(Hex.encodeHex(a1));
			String rspauth = md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce
					+ ":auth:" + ha2);
			if (resp == null) {
				resp = new BasicHttpResponse(_204);
			}
			String authenticate = "qop=auth,cnonce=\"" + cnonce + "\",nc=" + nc
					+ ",rspauth=\"" + rspauth + "\"";
			resp.setHeader("Authentication-Info", authenticate);
			break;
		}
		return resp;
	}

	@Override
	public Object authenticateRequest(String method, Object resource,
			Map<String, String[]> map) throws RepositoryException {
		String url = map.get("request-target")[0];
		String[] md5 = map.get("content-md5");
		String[] auth = map.get("authorization");
		if (auth == null || auth.length != 1 || auth[0] == null
				|| !auth[0].startsWith("Digest"))
			return null;
		try {
			String string = auth[0].substring("Digest ".length());
			Map<String, String> options = parseOptions(string);
			if (options == null)
				throw new BadRequest("Invalid digest authorization header");
			String realm = options.get("realm");
			String uri = options.get("uri");
			ParsedURI parsed = new ParsedURI(url);
			String path = parsed.getPath();
			if (parsed.getQuery() != null) {
				path = path + "?" + parsed.getQuery();
			}
			Object realmAuth = getAuthName();
			if (realmAuth == null) {
				logger.warn("Missing authName in {}", getResource());
				return null;
			}
			if (realm == null || !realm.equals(realmAuth.toString())
					|| !url.equals(uri) && !path.equals(uri)) {
				logger.info("Bad authorization on {} using {}", url, auth[0]);
				throw new BadRequest("Bad Authorization");
			}
			String qop = options.get("qop");
			if ("auth-int".equals(qop) && (md5 == null || md5.length != 1))
				throw new BadRequest("Missing content-md5");
			if (!verify(options.get("nonce"), resource, map.get("via")))
				return null;
			String md50 = md5 == null ? null : md5[0];
			return authenticatedCredential(method, md50, options);
		} catch (BadRequest e) {
			throw e;
		} catch (Exception e) {
			logger.warn(e.toString(), e);
			return null;
		}
	}

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, Map<String, String[]> map) {
		String url = map.get("request-target")[0];
		ParsedURI parsed = new ParsedURI(url);
		String query = parsed.getQuery();
		return credential.equals(resource)
				|| AuthorizeCredential(credential, method, resource, query);
	}

	public Object findCredential(String authorization) {
		if (authorization == null || !authorization.startsWith("Digest"))
			return null;
		String string = authorization.substring("Digest ".length());
		Map<String, String> options = parseOptions(string);
		String username = options.get("username");
		if (username == null)
			throw new BadRequest("Missing username");
		List<Object[]> encodings = findDigest(username);
		if (encodings.isEmpty())
			return null;
		return encodings.get(0)[0];
	}

	@sparql(PREFIX + "SELECT ?user ?encoded\n"
			+ "WHERE { ?user :name $name .\n"
			+ "$this :authenticates [:member ?user] .\n"
			+ "OPTIONAL { ?user :encoded ?encoded; :algorithm \"MD5\" } }")
	protected abstract List<Object[]> findDigest(@name("name") String username);

	protected abstract boolean AuthorizeCredential(Object credential,
			String method, Object object, String query);

	private Object authenticatedCredential(String method, String md5,
			Map<String, String> options) throws UnsupportedEncodingException {
		String ha2;
		String qop = options.get("qop");
		String uri = options.get("uri");
		String nonce = options.get("nonce");
		String username = options.get("username");
		String response = options.get("response");
		if ("auth-int".equals(qop)) {
			byte[] md5sum = Base64.decodeBase64(md5.getBytes("UTF-8"));
			char[] hex = Hex.encodeHex(md5sum);
			ha2 = md5(method + ":" + uri + ":" + new String(hex));
		} else {
			ha2 = md5(method + ":" + uri);
		}
		if (username == null)
			throw new BadRequest("Missing username");
		List<Object[]> encodings = findDigest(username);
		if (encodings.isEmpty()) {
			logger.info("Account not found: {}", username);
			failedAttempt(username);
			return null;
		}
		if (qop != null) {
			boolean replayed;
			synchronized (replay) {
				replayed = replay.put(options, Boolean.TRUE) != null;
			}
			if (replayed) {
				logger.info("Request replayed {}", options);
				failedAttempt(username);
				return null;
			}
		}
		boolean encoding = false;
		for (Object[] row : encodings) {
			byte[] a1 = (byte[]) row[1];
			if (a1 == null)
				continue;
			encoding = true;
			String ha1 = new String(Hex.encodeHex(a1));
			String legacy = ha1 + ":" + nonce + ":" + ha2;
			if (qop == null && md5(legacy).equals(response))
				return row[0];
			String expected = ha1 + ":" + nonce + ":" + options.get("nc") + ":"
					+ options.get("cnonce") + ":" + qop + ":" + ha2;
			if (md5(expected).equals(response))
				return row[0];
		}
		if (encoding) {
			logger.info("Passwords don't match for: {}", username);
			failedAttempt(username);
		} else {
			logger.info("Missing password for: {}", username);
			failedAttempt(username);
		}
		return null;
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

	private String nextNonce(Object resource, String[] key) {
		String ip = hash(key);
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

	private String hash(String[] key) {
		long code = 0;
		if (key != null) {
			for (String str : key) {
				code = code * 31 + str.hashCode();
			}
		}
		return Long.toString(code, Character.MAX_RADIX);
	}

	private boolean verify(String nonce, Object resource, String[] key) {
		if (nonce == null)
			return false;
		try {
			int first = nonce.indexOf(':');
			int last = nonce.lastIndexOf(':');
			if (first < 0 || last < 0)
				return false;
			if (!hash(key).equals(nonce.substring(last + 1)))
				return false;
			String revision = nonce.substring(first + 1, last);
			if (!revision.equals(getRevisionOf(resource)))
				return false;
			String time = nonce.substring(0, first);
			Long ms = Long.valueOf(time, Character.MAX_RADIX);
			long age = System.currentTimeMillis() - ms;
			return age < MAX_NONCE_AGE;
		} catch (NumberFormatException e) {
			logger.debug(e.toString(), e);
			return false;
		}
	}

	private String md5(String a2) {
		return new String(Hex.encodeHex(DigestUtils.md5(a2)));
	}

	/**
	 * 
	 * @param options
	 *            username="Mufasa", realm="testrealm@host.com",
	 *            nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
	 *            uri="/dir/index.html", qop=auth, nc=00000001,
	 *            cnonce="0a4f113b",
	 *            response="6629fae49393a05397450978507c4ef1"
	 */
	private Map<String, String> parseOptions(String options) {
		Map<String, String> result = new HashMap<String, String>(DIGEST_OPTS);
		Matcher m = TOKENS_REGEX.matcher(options);
		while (m.find()) {
			String key = m.group(1);
			if (result.containsKey(key)) {
				if (m.group(2) != null) {
					result.put(key, m.group(2));
				} else if (m.group(3) != null){
					result.put(key, m.group(3));
				}
			}
		}
		return result;
	}

}
