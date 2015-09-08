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

import static org.callimachusproject.util.PercentCodec.decode;
import static org.callimachusproject.util.PercentCodec.encode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.exceptions.BadGateway;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates HTTP digest authorization.
 */
public class CookieAuthenticationManager implements
		DetachedAuthenticationManager {
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");
	private static final BasicStatusLine _303 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 303, "See Other");
	private static final Pattern SID_SPLIT = Pattern
			.compile("^(.*?):(\\w*?):(\\w*?):(.*)$");
	private static final int LOGIN_GRP = 1;
	private static final int HASH_GRP = 2;
	private static final int NONCE_GRP = 3;
	private static final int IRI_GRP = 4;

	private final Logger logger = LoggerFactory
			.getLogger(CookieAuthenticationManager.class);
	private final String protectedPath;
	private final List<String> domains;
	private final Set<String> userCookies = new LinkedHashSet<String>();
	private final SecureRandom random = new SecureRandom();
	private final String identifier;
	private final byte[] secret;
	private final String redirect_prefix;
	private final String fullname_prefix;
	private final String secureCookie;
	private final String sid;
	private final ParameterAuthReader reader;

	public CookieAuthenticationManager(String identifier, String redirect_uri,
			String fullname_prefix, String path, List<String> domains,
			RealmManager realms, ParameterAuthReader reader)
			throws OpenRDFException, IOException {
		assert domains != null;
		assert domains.size() > 0;
		this.domains = domains;
		Set<Integer> ports = new HashSet<Integer>();
		for (String domain : domains) {
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
		assert reader != null;
		assert identifier != null;
		this.reader = reader;
		this.identifier = identifier;
		this.redirect_prefix = redirect_uri + "&return_to=";
		this.fullname_prefix = fullname_prefix;
		assert redirect_uri.contains("?");
		boolean secureOnly = identifier.startsWith("https");
		this.protectedPath = path;
		this.secureCookie = secureOnly ? ";Secure" : "";
		String hex = Integer.toHexString(Math.abs(identifier.hashCode()));
		this.sid = "sid" + hex + "=";
		String string = realms.getRealm(identifier).getOriginSecret();
		if (Base64.isBase64(string)) {
			this.secret = Base64.decodeBase64(string);
		} else {
			this.secret = string.getBytes(Charset.forName("UTF-8"));
		}
	}

	@Override
	public String toString() {
		return getIdentifier();
	}

	@Override
	public String getIdentifier() {
		return identifier;
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
		String url = getRequestUrl(request);
		String[] via = request.get("via");
		Collection<String> cookie = asList(request.get("cookie"));
		boolean loggedIn = isCookiePresent(sid, cookie);
		HttpResponse resp = getLoginResponse(loggedIn, method, url, via, body);
		if (!resp.containsHeader("Cache-Control")) {
			resp.setHeader("Cache-Control", "no-store");
		}
		return resp;
	}

	@Override
	public HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con)
			throws OpenRDFException, IOException {
		// no authentication info
		return null;
	}

	@Override
	public String authenticateRequest(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con) {
		try {
			Collection<String> cookies = asList(request.get("cookie"));
			Matcher cookie = getVerifiedCookie(method, sid, cookies, SID_SPLIT);
			if (cookie == null)
				return null;
			return cookie.group(IRI_GRP);
		} catch (BadRequest e) {
			throw e;
		} catch (Exception e) {
			logger.warn(e.toString(), e);
			return null;
		}
	}

	@Override
	public HttpResponse logout(Collection<String> tokens) {
		HttpResponse resp = getLogoutResponse();
		for (String userCookie : userCookies) {
			String secure = userCookie.endsWith("s=") ? ";Secure" : "";
			resp.addHeader("Set-Cookie", userCookie + ";Max-Age=0;Path="
					+ protectedPath + secure);
		}
		return resp;
	}

	public String[] getUsernameSetCookie(Collection<String> tokens,
			ObjectConnection con) {
		String username = getUserLogin(tokens, con);
		return getUsernameSetCookie(username);
	}

	public String getUserIdentifier(String method, Collection<String> tokens,
			ObjectConnection con) throws OpenRDFException, IOException {
		Matcher cookie = getParsedCookie(sid, tokens, SID_SPLIT);
		if (cookie == null)
			return null;
		return cookie.group(IRI_GRP);
	}

	public String getUserLogin(Collection<String> tokens, ObjectConnection con) {
		Matcher cookie = getParsedCookie(sid, tokens, SID_SPLIT);
		if (cookie == null)
			return null;
		return cookie.group(LOGIN_GRP);
	}

	@Override
	public void registered(Resource invitedUser, URI registeredUser,
			ObjectConnection con) throws OpenRDFException {
		// welcome
	}

	private HttpResponse getLoginResponse(boolean loggedIn, String method,
			String url, String[] via, HttpEntity body) throws IOException {
		String target = url;
		String parameters = null;
		if (target.startsWith(redirect_prefix)) {
			String uri = target;
			String query = "";
			int idx = target.indexOf('&', redirect_prefix.length());
			if (idx > 0) {
				uri = target.substring(0, idx);
				query = target.substring(idx + 1);
			}
			target = getValueAfter(target, redirect_prefix);
			parameters = reader.getParameters(method, uri, query, body);
			if (reader.isCanncelled(parameters))
				throw new BadGateway("Could not login");
			if (reader.isLoggingIn(parameters)) {
				if (verify(parameters, via))
					return redirectReturnTo(target, parameters);
				throw new BadGateway("Invalid login");
			}
		}
		String location = reader.getLoginPage(redirect_prefix + encode(target),
				loggedIn, parameters, via);
		BasicHttpResponse resp = new BasicHttpResponse(_303);
		resp.addHeader("Location", location);
		resp.addHeader("Set-Cookie", sid + ";Max-Age=0;HttpOnly;Path="
				+ protectedPath + secureCookie);
		return resp;
	}

	private String getValueAfter(String parameters, String token) {
		if (parameters == null)
			return null;
		int idx = parameters.indexOf(token);
		if (idx < 0)
			return null;
		int start = idx + token.length();
		int end = parameters.indexOf('&', start);
		if (end < 0) {
			end = parameters.length();
		}
		return decode(parameters.substring(start, end));
	}

	private boolean verify(String parameters, String[] via) {
		if (parameters == null)
			return false;
		try {
			if (reader.isValidParameters(parameters, via)) {
				return true;
			} else {
				return false;
			}
		} catch (Exception io) {
			logger.warn(io.toString(), io);
			return false;
		}
	}

	private HttpResponse redirectReturnTo(String url, String parameters)
			throws IOException {
		if (url.startsWith(fullname_prefix)) {
			// if this is a registration URL, update the fullname
			String fullname = reader.getUserFullName(parameters);
			String param = "&fullname=" + encode(fullname);
			if(url.contains("&fullname=")) {
				url.replaceAll("\\&fullname=[^\\&]*", param);
			} else if (url.contains("#")) {
				url.replaceFirst("#", param + "#");
			} else {
				url = url + param;
			}
		}
		String username = reader.getUserLogin(parameters);
		String iri = reader.getUserIdentifier(parameters);
		String userInfo = getSidCookieValue(username, iri);
		BasicHttpResponse resp = new BasicHttpResponse(_303);
		resp.addHeader("Location", url);
		String encoded = encode(userInfo).replace("%2F", "/").replace("%3A",
				":");
		resp.addHeader("Set-Cookie", sid + encoded + ";HttpOnly;Path="
				+ protectedPath + secureCookie);
		for (String cookie : getUsernameSetCookie(username)) {
			resp.addHeader("Set-Cookie", cookie);
		}
		return resp;
	}

	private String[] getUsernameSetCookie(String username) {
		if (username == null)
			return new String[0];
		int i = 0;
		String[] result = new String[userCookies.size()];
		for (String userCookie : userCookies) {
			String secure = userCookie.endsWith("s=") ? ";Secure" : "";
			result[i++] = userCookie + encode(username) + ";Path="
					+ protectedPath + secure;
		}
		return result;
	}

	private String getSidCookieValue(String login, String iri)
			throws IOException {
		String nonce = nextNonce();
		int hour = getTimeSlot(System.currentTimeMillis());
		String hash = getPassword(hour, login, iri, nonce);
		return login + ":" + hash + ":" + nonce + ":" + iri;
	}

	private HttpResponse getLogoutResponse() {
		BasicHttpResponse resp = new BasicHttpResponse(_204);
		resp.addHeader("Set-Cookie", sid + ";Max-Age=0;HttpOnly;Path="
				+ protectedPath + secureCookie);
		return resp;
	}

	private Matcher getVerifiedCookie(String method, String name,
			Collection<String> cookies, Pattern pattern) throws IOException {
		Matcher m = getParsedCookie(name, cookies, pattern);
		if (m == null)
			return null;
		String nonce = m.group(NONCE_GRP);
		String hash = m.group(HASH_GRP);
		String username = m.group(LOGIN_GRP);
		String iri = m.group(IRI_GRP);
		int hour = getTimeSlot(System.currentTimeMillis());
		int leeway = hour - 3;
		if (!"GET".equals(method)) {
			leeway -= 6;
		}
		for (int h = hour; h >= leeway; h--) {
			String password = getPassword(h, username, iri, nonce);
			if (hash.equals(password))
				return m;
		}
		return m;
	}

	private Matcher getParsedCookie(String name, Collection<String> cookies,
			Pattern pattern) {
		String cookie = getCookie(name, cookies);
		if (cookie == null)
			return null;
		Matcher matcher = pattern.matcher(cookie);
		if (matcher.find())
			return matcher;
		return null;
	}

	private String getCookie(String token, Collection<String> cookies) {
		if (cookies == null)
			return null;
		for (String cookie : cookies) {
			if (!cookie.contains(token))
				continue;
			for (String p : cookie.split("\\s*;\\s*")) {
				if (p.startsWith(token)) {
					String raw = p.substring(token.length());
					if (raw.length() == 0)
						return null;
					return decode(raw);
				}
			}
		}
		return null;
	}

	private boolean isCookiePresent(String name, Collection<String> cookies) {
		return getCookie(sid, cookies) != null;
	}

	private String nextNonce() {
		synchronized (random) {
			return Long.toString(Math.abs(random.nextLong()),
					Character.MAX_RADIX);
		}
	}

	private int getTimeSlot(long now) {
		long slot = now / 1000 / 60 / 5;
		return (int) slot;
	}

	private String getPassword(int hour, String username, String iri,
			String nonce) throws IOException {
		int size = secret.length + username.length() + iri.length()
				+ nonce.length();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(size * 2);
		baos.write(secret);
		for (int i = 0, n = Integer.SIZE / Byte.SIZE; i < n; i++) {
			baos.write((byte) hour);
			hour >>= Byte.SIZE;
		}
		baos.write(getIdentifier().getBytes("UTF-8"));
		baos.write(username.getBytes("UTF-8"));
		baos.write(iri.getBytes("UTF-8"));
		baos.write(nonce.getBytes("UTF-8"));
		return new String(Hex.encodeHex(DigestUtils.md5(baos.toByteArray())));
	}

	private String getRequestUrl(Map<String, String[]> request) {
		String target = request.get("request-target")[0];
		if (target.charAt(0) != '/')
			return target;
		String scheme = request.get("request-scheme")[0];
		String host = request.get("host")[0];
		return scheme + "://" + host + target;
	}

	private Collection<String> asList(String[] array) {
		if (array == null)
			return null;
		return Arrays.asList(array);
	}

}
