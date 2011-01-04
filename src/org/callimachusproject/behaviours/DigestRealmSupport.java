/*
 * Copyright (c) 2010, Zepheira LLC and James Leigh Some rights reserved.
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.concepts.DigestRealm;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.annotations.name;
import org.openrdf.repository.object.annotations.sparql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates HTTP digest authorization.
 */
public abstract class DigestRealmSupport extends RealmSupport implements DigestRealm, RDFObject {
	private static final String PREFIX = "PREFIX :<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final BasicStatusLine _401 = new BasicStatusLine(
			new ProtocolVersion("HTTP", 1, 1), 401, "Unauthorized");
	private static final int MAX_NONCE_AGE = 12;
	private static final TimeUnit MAX_NONCE_AGE_UNIT = TimeUnit.HOURS;
	private Logger logger = LoggerFactory.getLogger(DigestRealmSupport.class);

	@Override
	public HttpResponse unauthorized(Object target, String query) throws Exception {
		Object realm = getAuthName();
		if (realm == null)
			return forbidden(target, query);
		String domain = protectionDomain();
		if (domain == null) {
			domain = "";
		} else if (domain.length() != 0) {
			domain = ", domain=\"" + domain + "\"";
		}
		String nonce = nextNonce();
		HttpResponse resp = super.unauthorized(target, query);
		if (resp == null) {
			resp = new BasicHttpResponse(_401);
			resp.setHeader("Cache-Control", "no-store");
			resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
			resp.setEntity(new StringEntity("Unauthorized", "UTF-8"));
		}
		String authenticate = "Digest realm=\"" + realm + "\"" + domain
				+ ", nonce=\"" + nonce
				+ "\", algorithm=\"MD5\", qop=\"auth,auth-int\"";
		resp.setHeader("WWW-Authenticate", authenticate);
		return resp;
	}

	@Override
	public Object authenticateRequest(String method, Object resource,
			Map<String, String[]> map) throws RepositoryException {
		String url = map.get("request-target")[0];
		String[] md5 = map.get("content-md5");
		String[] auth = map.get("authorization");
		if (auth == null || auth.length != 1 || auth[0] == null || !auth[0].startsWith("Digest"))
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
			if (!verify(options.get("nonce"))) {
				logger.info("Invalid Authorization");
				return null;
			}
			String qop = options.get("qop");
			if ("auth-int".equals(qop) && (md5 == null || md5.length != 1))
				throw new BadRequest("Missing content-md5");
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
			Object resource, String qs) {
		return credential.equals(resource)
				|| AuthorizeCredential(credential, method, resource, qs);
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
			return null;
		}
		boolean encoding = false;
		for (Object[] row : encodings) {
			byte[] a1 = (byte[]) row[1];
			if (a1 == null)
				continue;
			encoding = true;
			String ha1 = new String(Hex.encodeHex(a1));
			String legacy = ha1 + ":" + nonce + ":" + ha2;
			if (md5(legacy).equals(response))
				return row[0];
			String expected = ha1 + ":" + nonce + ":" + options.get("nc") + ":"
					+ options.get("cnonce") + ":" + qop + ":" + ha2;
			if (md5(expected).equals(response))
				return row[0];
		}
		if (encoding) {
			logger.info("Passwords don't match for: {}", username);
		} else {
			logger.info("Missing password for: {}", username);
		}
		return null;
	}

	private String nextNonce() {
		return Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
	}

	private boolean verify(String nonce) {
		if (nonce == null)
			return false;
		try {
			Long time = Long.valueOf(nonce, Character.MAX_RADIX);
			long age = System.currentTimeMillis() - time;
			return age < MAX_NONCE_AGE_UNIT.toMillis(MAX_NONCE_AGE);
		} catch (NumberFormatException e) {
			logger.debug(e.toString(), e);
			return false;
		}
	}

	private String md5(String a2) {
		return new String(Hex.encodeHex(DigestUtils.md5(a2)));
	}

	private Map<String, String> parseOptions(String options) {
		Map<String, String> result = new HashMap<String, String>();
		// FIXME the URI might have a comma in it
		for (String keyvalue : options.split("\\s*,\\s*")) {
			int idx = keyvalue.indexOf('=');
			if (idx < 0)
				return null;
			String key = keyvalue.substring(0, idx);
			if (idx == keyvalue.length() - 1) {
				result.put(key, "");
			} else if (keyvalue.charAt(idx + 1) == '"') {
				int eq = keyvalue.lastIndexOf('"');
				if (eq <= idx + 2)
					return null;
				String value = keyvalue.substring(idx + 2, eq);
				result.put(key, value);
			} else {
				String value = keyvalue.substring(idx + 1);
				result.put(key, value);
			}
		}
		return result;
	}

}
