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

import info.aduna.net.ParsedURI;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.traits.VersionedObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates HTTP digest authorization.
 */
public class DigestManager implements AuthenticationManager {
	private static final String USERNAME = "username=";
	private static final BasicStatusLine _401 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 401, "Unauthorized");
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");
	private static final int MAX_NONCE_AGE = 300000; // nonce timeout of 5min
	private static final int MAX_ENTRIES = 2048;
	private static long resetAttempts;
	private static final Map<String, Integer> failedAttempts = new HashMap<String, Integer>();
	private static final Map<Map<String, String>, Object> replay = new LinkedHashMap<Map<String, String>, Object>() {
		private static final long serialVersionUID = -6673793531014489904L;

		protected boolean removeEldestEntry(
				Entry<Map<String, String>, Object> eldest) {
			return size() > MAX_ENTRIES;
		}
	};
	private final Logger logger = LoggerFactory.getLogger(DigestManager.class);
	private final Resource self;
	private final String authName;
	private final String protectedDomains;
	private final DigestHelper helper = new DigestHelper();

	public DigestManager(Resource self) {
		this(self, null, null);
	}

	public DigestManager(Resource self, String authName, String protectedDomains) {
		assert self != null;
		this.self = self;
		this.authName = authName;
		this.protectedDomains = protectedDomains;
	}

	@Override
	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) {
		if (authName == null)
			return null;
		String domain = protectedDomains;
		if (domain == null) {
			domain = "";
		} else if (domain.length() != 0) {
			domain = ", domain=\"" + domain + "\"";
		}
		String nonce = nextNonce(resource, request.get("via"));
		String authenticate = "Digest realm=\"" + authName + "\"" + domain
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
		String ha2 = helper.md5(":" + uri);
		String rspauth = helper.md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce
				+ ":auth:" + ha2);
		BasicHttpResponse resp = new BasicHttpResponse(_204);
		String authenticate = "qop=auth,cnonce=\"" + cnonce + "\",nc=" + nc
				+ ",rspauth=\"" + rspauth + "\"";
		resp.addHeader("Authentication-Info", authenticate);
		resp.addHeader("Set-Cookie", USERNAME + encode(username) + ";Path=/");
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
	public HttpResponse logout(Collection<String> tokens) {
		if (authName == null)
			return null;
		for (String token : tokens) {
			if (token.indexOf("username=\"logout\"") > 0) {
				// # bogus credentials received
				BasicHttpResponse resp = new BasicHttpResponse(_204);
				resp.addHeader("Set-Cookie", helper.clearCookie());
				resp.addHeader("Set-Cookie", USERNAME + ";Max-Age=0;Path=/");
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
		Map<String,String> passwords = helper.findDigestUser(username, realm, asList(request.get("cookie")), self, con);
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
		if (realm == null || !realm.equals(authName)
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

	private String hash(String[] via) {
		long code = 0;
		if (via != null) {
			for (String str : via) {
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
