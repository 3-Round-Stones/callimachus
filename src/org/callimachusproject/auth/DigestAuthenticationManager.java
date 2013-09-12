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

import static org.callimachusproject.util.PercentCodec.encode;
import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates HTTP digest authorization.
 */
public class DigestAuthenticationManager implements DetachedAuthenticationManager {
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|([^,\"]*)))?\\s*,?");
	private static final int MAX_NONCE_AGE = 300000; // nonce timeout of 5min
	private static final BasicStatusLine _401 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 401, "Unauthorized");
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");
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
	private final Logger logger = LoggerFactory.getLogger(DigestAuthenticationManager.class);
	private final String authName;
	private final String protectedDomains;
	private final String protectedPath;
	private final List<String> domains;
	private final FailManager fail = new FailManager();
	private final Set<String> userCookies = new LinkedHashSet<String>();
	private final DigestAccessor accessor;

	public DigestAuthenticationManager(String authName, String path,
			List<String> domains, DigestAccessor accessor) {
		assert authName != null;
		assert accessor != null;
		this.authName = authName;
		this.protectedPath = path;
		assert domains != null;
		assert domains.size() > 0;
		this.domains = domains;
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
			}
			suffix.append('=');
			userCookies.add("username" + suffix);
		}
		this.protectedDomains = sb.substring(1);
		this.accessor = accessor;
	}

	@Override
	public String toString() {
		return getDigestAccessor().getIdentifier();
	}

	@Override
	public String getIdentifier() {
		return getDigestAccessor().getIdentifier();
	}

	public DigestAccessor getDigestAccessor() {
		return accessor;
	}

	public boolean isProtected(String url) {
		for (String domain : domains) {
			if (url.startsWith(domain))
				return true;
		}
		return false;
	}

	@Override
	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request, HttpEntity body) throws IOException {
		String nonce = nextNonce(resource, request.get("via"));
		String authenticate = "Digest realm=\"" + authName + "\""
				+ ", domain=\"" + protectedDomains + "\"" + ", nonce=\""
				+ nonce + "\", algorithm=\"MD5\", qop=\"auth\"";
		Map<String, String> options = parseDigestAuthorization(request);
		HttpResponse resp;
		if (options == null && isUserLoggedIn(request)) {
			resp = new BasicHttpResponse(_401);
			resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
			try {
				resp.setEntity(new StringEntity("Authorization header required", "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new AssertionError(e);
			}
		} else if (options != null && !isRecentDigest(resource, request, options)) {
			resp = new BasicHttpResponse(_401);
			resp.setHeader("WWW-Authenticate", authenticate + ",stale=true");
			resp.setHeader("Content-Type", "text/plain;charset=\"UTF-8\"");
			try {
				resp.setEntity(new StringEntity("Stale authorization header", "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new AssertionError(e);
			}
		} else {
			String url = getRequestUrl(request);
			String[] via = request.get("via");
			Collection<String> cookie = asList(request.get("cookie"));
			if (options == null) {
				 resp = accessor.getNotLoggedInResponse(method, url, via, cookie, body);
			} else {
				resp = accessor.getBadCredentialResponse(method, url, via, cookie, body);
			}
		}
		if (!resp.containsHeader("Cache-Control")) {
			resp.setHeader("Cache-Control", "no-store");
		}
		if (resp.getStatusLine().getStatusCode() == 401
				&& !resp.containsHeader("WWW-Authenticate")) {
			resp.setHeader("WWW-Authenticate", authenticate);
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
				HttpResponse resp = getDigestAccessor().getLogoutResponse();
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

	public String getUserIdentifier(String method, Collection<String> tokens,
			ObjectConnection con) throws OpenRDFException, IOException {
		Map<String, String> options = parseDigestAuthorization(tokens);
		if (options == null)
			return null;
		String username = options.get("username");
		if (username == null)
			throw new BadRequest("Missing username");
		Map.Entry<String, String> passwords = findAuthUser(method, options,
				tokens, con);
		if (passwords == null)
			return null;
		return passwords.getValue();
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

	@Override
	public void registered(Resource invitedUser, URI registeredUser,
			ObjectConnection con) throws OpenRDFException, IOException {
		getDigestAccessor().registerUser(invitedUser, registeredUser, con);
	}

	private Map.Entry<String, String> findAuthUser(String method,
			Object resource, Map<String, String[]> request,
			Map<String, String> auth, ObjectConnection con)
			throws OpenRDFException, IOException {
		if (!isRecentDigest(resource, request, auth))
			return null;
		Collection<String> cookies = asList(request.get("cookie"));
		return findAuthUser(method, auth, cookies, con);
	}

	private Map.Entry<String, String> findAuthUser(String method,
			Map<String, String> auth, Collection<String> cookies,
			ObjectConnection con) throws OpenRDFException, IOException {
		String qop = auth.get("qop");
		String uri = auth.get("uri");
		String nonce = auth.get("nonce");
		String username = auth.get("username");
		String realm = auth.get("realm");
		String response = auth.get("response");
		String ha2 = md5(method + ":" + uri);
		assert username != null;
		DigestAccessor accessor = getDigestAccessor();
		Map<String, String> passwords = accessor.findDigestUser(method,
				username, realm, cookies, con);
		if (passwords == null) {
			logger.debug("Account {} not found in {}", username, getIdentifier());
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

	private String nextNonce(Object resource, String[] via) {
		String ip = hash(via);
		String revision = getRevisionOf(resource);
		long now = System.currentTimeMillis();
		String time = Long.toString(now, Character.MAX_RADIX);
		return time + ":" + revision + ":" + ip;
	}

	private String getRevisionOf(Object resource) {
		if (resource instanceof CalliObject) {
			String revision = ((CalliObject) resource).revision();
			if (revision != null)
				return revision;
		}
		return "";
	}

	private Map<String, String> parseDigestAuthorization(
			Map<String, String[]> request) {
		return parseDigestAuthorization(asList(request.get("authorization")));
	}

	private Map<String, String> parseDigestAuthorization(
			Collection<String> authorization) {
		if (authorization == null)
			return null;
		for (String digest : authorization) {
			if (digest == null || !digest.startsWith("Digest "))
				continue;
			if (digest.indexOf("username=\"-\"") > 0)
				continue; // bogus
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

	private String getRequestUrl(Map<String, String[]> request) {
		String target = request.get("request-target")[0];
		if (target.charAt(0) != '/')
			return target;
		String scheme = request.get("request-scheme")[0];
		String host = request.get("host")[0];
		return scheme + "://" + host + target;
	}

	private boolean isUserLoggedIn(Map<String, String[]> request) {
		if (request.get("cookie") == null)
			return false;
		for (String cookie : request.get("cookie")) {
			for (String cookieName : userCookies) {
				if (cookie.contains(cookieName)) {
					int start = cookie.indexOf(cookieName) + cookieName.length();
					if (start >= cookie.length())
						return false;
					int semi = cookie.indexOf(';', start);
					int comma = cookie.indexOf(',', start);
					int end = comma < 0 || semi < comma && semi > 0 ? semi : comma;
					if (end < 0) {
						end = cookie.length();
					}
					String value = cookie.substring(start, end).trim();
					return value.length() > 0;
				}
			}
		}
		return false;
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

}
