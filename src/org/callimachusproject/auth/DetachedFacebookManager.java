/*
 * Copyright (c) 2012, James Leigh Some rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.BadGateway;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.util.StringUrlCodec;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetachedFacebookManager implements DetachedAuthenticationManager {
	private static final String HAS_COMPONENT = "http://callimachusproject.org/rdf/2009/framework#hasComponent";
	private static final String PARTY = "http://callimachusproject.org/rdf/2009/framework#Party";
	private static final String USER = "http://callimachusproject.org/rdf/2009/framework#User";
	private static final String EMAIL = "http://callimachusproject.org/rdf/2009/framework#email";
	private static final String PROV = "http://www.w3.org/ns/prov#";
	private static final String OAUTH_URL = "https://www.facebook.com/dialog/oauth";
	private static final String ACCESS_URL = "https://graph.facebook.com/oauth/access_token";
	private static final String ME_URL = "https://graph.facebook.com/me";
	private static final BasicStatusLine _204 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 204, "No Content");
	private static final BasicStatusLine _303 = new BasicStatusLine(
			HttpVersion.HTTP_1_1, 303, "See Other");

	private final Logger logger = LoggerFactory.getLogger(DetachedFacebookManager.class);
	private final String identifier;
	private final String redirect_uri;
	private final StringUrlCodec codec;
	private final String protectedPath;
	private final String appId;
	private final String secret;
	private final String fbTokenSecure;
	private final String fbToken;
	private final Set<String> userCookies = new LinkedHashSet<String>();
	private final Map<String, AccessToken> tokens = new HashMap<String, AccessToken>();

	public DetachedFacebookManager(String identifier, String redirect_uri,
			String appId, CharSequence secret, String path, List<String> domains) {
		this.identifier = identifier;
		this.redirect_uri = redirect_uri;
		assert redirect_uri.contains("?");
		this.codec = new StringUrlCodec();
		this.appId = appId;
		this.secret = secret.toString();
		this.protectedPath = path;
		boolean secureOnly = true;
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
			} else {
				secureOnly = false;
			}
			suffix.append('=');
			userCookies.add("username" + suffix);
		}
		this.fbTokenSecure = secureOnly ? ";Secure" : "";
		StringBuilder dn = new StringBuilder();
		dn.append("fbToken");
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
		this.fbToken = dn.toString();
	}

	@Override
	public String toString() {
		return getIdentifier();
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	public String getUserIdentifier(Collection<String> cookies) {
		AccessToken token = getAccessToken(cookies);
		if (token == null)
			return null;
		return token.getLink();
	}

	public String getUserLogin(Collection<String> cookies) {
		String cookie = getCookie(cookies);
		if (cookie != null) {
			return getAccessToken(cookies).getLogin();
		}
		return null;
	}

	@Override
	public String authenticateRequest(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con) {
		String cookie = getCookie(asList(request.get("cookie")));
		if (cookie != null) {
			String[] via = request.get("via");
			AccessToken token = getAccessToken(cookie, via);
			if (token != null) {
				return token.getLink();
			}
		}
		return null;
	}

	@Override
	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) {
		String[] via = request.get("via");
		String target = getRequestUrl(request);
		int count = 0;
		if (isRedirectUrl(target)) {
			if (target.contains("&state=")) {
				HttpResponse resp = getReturnToResponse(target, via);
				if (resp != null)
					return resp;
				if (!target.contains("&state=0"))
					throw new BadGateway("Could not login via Facebook");
				count = 1;
			}
			target = getReturnTo(target);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(OAUTH_URL);
		sb.append("?client_id=").append(encode(getFacebookAppId()));
		sb.append("&redirect_uri=").append(encode(createRedirectUrl(target)));
		sb.append("&state=").append(encode(state(count, via)));
		if (count == 0) {
			sb.append("&scope=email");
		}
		BasicHttpResponse resp = new BasicHttpResponse(_303);
		resp.addHeader("Location", sb.toString());
		resp.addHeader("Set-Cookie", fbToken + ";Max-Age=0;Path=/;HttpOnly"
				+ fbTokenSecure);
		return resp;
	}

	public String[] getUsernameSetCookie(Collection<String> cookies) {
		String email = getUserLogin(cookies);
		if (email == null)
			return new String[0];
		int i = 0;
		String[] result = new String[userCookies.size()];

		for (String userCookie : userCookies) {
			String secure = userCookie.endsWith("s=") ? ";Secure" : "";
			result[i++] = userCookie + encode(email) + ";Max-Age=2678400;Path="
					+ protectedPath + secure;
		}
		return result;
	}

	@Override
	public HttpResponse logout(Collection<String> tokens) {
		boolean loggedIn = false;
		loop: for (String token : tokens) {
			if (token.indexOf(fbToken) >= 0) {
				loggedIn = true;
				break loop;
			}
		}
		if (!loggedIn)
			return null;
		BasicHttpResponse resp = new BasicHttpResponse(_204);
		resp.addHeader("Set-Cookie", fbToken + ";Max-Age=0;Path=/;HttpOnly"
				+ fbTokenSecure);
		for (String userCookie : userCookies) {
			String secure = userCookie.endsWith("s=") ? ";Secure" : "";
			resp.addHeader("Set-Cookie", userCookie + ";Max-Age=0;Path="
					+ protectedPath + secure);
		}
		return resp;
	}

	@Override
	public HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con)
			throws OpenRDFException {
		return null;
	}

	public void registerUser(Resource user, Collection<String> cookies,
			ObjectConnection con) throws OpenRDFException {
		AccessToken token = getAccessToken(cookies);
		if (token == null)
			throw new BadRequest("Missing authentication token");
		ValueFactory vf = con.getValueFactory();
		URI link = vf.createURI(token.getLink());
		URI hasEmail = vf.createURI(EMAIL);
		Literal label = vf.createLiteral(token.getName());
		String email = token.getEmail();
		RepositoryResult<Statement> stmts;
		stmts = con.getStatements((Resource) null, null, user);
		try {
			while (stmts.hasNext()) {
				moveTo(link, stmts.next(), con);
			}
		} finally {
			stmts.close();
		}
		stmts = con.getStatements(user, hasEmail, null);
		try {
			while (stmts.hasNext()) {
				Value obj = stmts.next().getObject();
				if (!con.hasStatement(link, hasEmail, obj)) {
					con.add(link, hasEmail, obj);
				}
			}
		} finally {
			stmts.close();
		}
		con.remove(user, null, null);
		add(link, RDF.TYPE, vf.createURI(PARTY), con);
		add(link, RDF.TYPE, vf.createURI(USER), con);
		if (!con.hasStatement(link, RDFS.LABEL, label)) {
			con.remove(link, RDFS.LABEL, null);
			con.add(link, RDFS.LABEL, label);
		}
		if (email != null) {
			Literal mailto = vf.createLiteral(email);
			if (!con.hasStatement(link, hasEmail, mailto)) {
				con.add(link, hasEmail, mailto);
			}
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

	private String getRequestUrl(Map<String, String[]> request) {
		String target = request.get("request-target")[0];
		if (target.charAt(0) != '/')
			return target;
		String scheme = request.get("request-scheme")[0];
		String host = request.get("host")[0];
		return scheme + "://" + host + target;
	}

	private HttpResponse getReturnToResponse(String url, String[] via) {
		AccessToken token = getAccessToken(url, via);
		if (token == null)
			return null;
		BasicHttpResponse resp = new BasicHttpResponse(_303);
		resp.addHeader("Location", getReturnTo(url));
		String cookie = fbToken
				+ codec.encode(url.substring(redirect_uri.length()));
		resp.addHeader("Set-Cookie", cookie + ";Path=/;HttpOnly"
				+ fbTokenSecure);
		for (String setCookie : getUsernameSetCookie(asList(cookie))) {
			resp.addHeader("Set-Cookie", setCookie);
		}
		return resp;
	}

	private boolean isRedirectUrl(String target) {
		return target.startsWith(redirect_uri + "&return_to=");
	}

	private String createRedirectUrl(String return_to) {
		return redirect_uri + "&return_to=" + encode(return_to);
	}

	private String getRedirectUri(String redirect_url) {
		int end = redirect_url.indexOf("&return_to=");
		end = redirect_url.indexOf('&', end + 1);
		if (end < 0) {
			end = redirect_url.length();
		}
		return redirect_url.substring(0, end);
	}

	private AccessToken verify(String redirect_url, String[] via) {
		boolean verified = false;
		String code = null;
		String error_description = null;
		String qs = redirect_url.substring(redirect_url.indexOf('?') + 1);
		for (String pair : qs.split("&")) {
			if (pair.startsWith("state=")) {
				String encoded = pair.substring("state=".length());
				verified |= verifyState(decode(encoded), via);
			} else if (pair.startsWith("code=")) {
				String en = pair.substring("code=".length());
				code = decode(en);
			} else if (pair.startsWith("error_description=")) {
				String en = pair.substring("error_description=".length());
				error_description = decode(en);
			}
		}
		if (error_description != null && error_description.length() > 0) {
			logger.warn("Facebook: {}", error_description);
			return null;
		} else if (verified && code != null) {
			return verifyCode(getRedirectUri(redirect_url), code);
		} else if (!verified) {
			logger.error("Invalid facebook manager state");
			return null;
		} else {
			logger.error("Could not login facebook user");
			return null;
		}
	}

	private String getCookie(Collection<String> cookies) {
		if (cookies != null) {
			for (String cookie : cookies) {
				if (!cookie.contains(fbToken))
					continue;
				String[] pair = cookie.split("\\s*;\\s*");
				for (String p : pair) {
					if (p.startsWith(fbToken)) {
						String raw = p.substring(fbToken.length());
						return redirect_uri + codec.decode(raw);
					}
				}
			}
		}
		return null;
	}

	private String getReturnTo(String url) {
		String qs = url.substring(url.indexOf('?') + 1);
		for (String pair : qs.split("&")) {
			if (pair.startsWith("return_to=")) {
				String en = pair.substring("return_to=".length());
				return decode(en);
			}
		}
		return null;
	}

	private AccessToken verifyCode(String redirect_uri, String code) {
		long now = System.currentTimeMillis();
		synchronized (tokens) {
			Iterator<AccessToken> iter = tokens.values().iterator();
			while (iter.hasNext()) {
				AccessToken token = iter.next();
				if (token.isExpired(now)) {
					iter.remove();
				}
			}
			if (tokens.containsKey(redirect_uri))
				return tokens.get(redirect_uri);
		}
		String url = ACCESS_URL + "?client_id="
				+ encode(getFacebookAppId()) + "&redirect_uri="
				+ encode(redirect_uri) + "&client_secret="
				+ encode(getFacebookSecret()) + "&code=" + encode(code);
		try {
			HTTPObjectClient client = HTTPObjectClient.getInstance();
			HttpResponse resp = client.get(url);
			HttpEntity entity = resp.getEntity();
			try {
				Scanner scanner = new Scanner(entity.getContent(), "UTF-8");
				String content = scanner.useDelimiter("\\A").next();
				AccessToken token = new AccessToken(now, content);
				synchronized (tokens) {
					tokens.put(redirect_uri, token);
					return token;
				}
			} finally {
				EntityUtils.consume(entity);
			}
		} catch (Exception e) {
			logger.error(url, e);
			return null;
		}
	}

	private AccessToken getAccessToken(Collection<String> cookies) {
		String cookie = getCookie(cookies);
		if (cookie == null)
			return null;
		AccessToken token = null;
		synchronized (tokens) {
			token = tokens.get(getRedirectUri(cookie));
		}
		if (token == null)
			return null;
		return token;
	}

	private AccessToken getAccessToken(String redirect_url, String[] via) {
		AccessToken token = null;
		synchronized (tokens) {
			token = tokens.get(getRedirectUri(redirect_url));
		}
		if (token == null) {
			token = verify(redirect_url, via);
		}
		if (token == null)
			return null;
		return token;
	}

	private String getFacebookAppId() {
		return appId;
	}

	private String getFacebookSecret() {
		return secret;
	}

	private String state(int count, String[] via) {
		StringBuilder sb = new StringBuilder();
		for (String v : via) {
			sb.append(v);
		}
		sb.append(secret);
		String str = Integer.toString(count, Character.MAX_RADIX);
		char chr = str.charAt(str.length() - 1);
		return chr + DigestUtils.md5Hex(sb.toString());
	}

	private boolean verifyState(String decode, String[] via) {
		return decode.substring(1).equals(state(0, via).substring(1));
	}

	private Collection<String> asList(String... array) {
		if (array == null)
			return null;
		return Arrays.asList(array);
	}

	private String encode(String username) {
		try {
			return URLEncoder.encode(username, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private String decode(String username) {
		try {
			return URLDecoder.decode(username, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private class AccessToken {
		private String access;
		private long expires;
		private JSONObject info;

		private AccessToken(long now, String access_token) {
			for (String pair : access_token.split("&")) {
				if (pair.startsWith("access_token=")) {
					String en = pair.substring("access_token=".length());
					access = decode(en);
				} else if (pair.startsWith("expires=")) {
					String en = pair.substring("expires=".length());
					long age = Long.parseLong(decode(en));
					expires =  now + age;
				}
			}
		}

		public String getAccess() {
			return access;
		}

		public String getLink() {
			try {
				return getInfo().getString("link");
			} catch (JSONException e) {
				throw new BadGateway(e);
			}
		}

		public String getEmail() {
			try {
				JSONObject info = getInfo();
				return info.getString("email");
			} catch (JSONException e) {
				throw new BadGateway(e);
			}
		}

		public String getName() {
			try {
				JSONObject info = getInfo();
				return info.getString("name");
			} catch (JSONException e) {
				throw new BadGateway(e);
			}
		}

		public String getLogin() {
			try {
				JSONObject info = getInfo();
				String email = info.optString("email");
				if (email != null)
					return email;
				return info.getString("username");
			} catch (JSONException e) {
				throw new BadGateway(e);
			}
		}

		/**
		 * middle_name link, locale, updated_time, id, first_name, username,
		 * timezone, verified, name, last_name, gender, email (optional)
		 */
		private synchronized JSONObject getInfo() {
			if (info != null)
				return info;
			try {
				String access = this.getAccess();
				HTTPObjectClient client = HTTPObjectClient.getInstance();
				String url = ME_URL + "?access_token=" + encode(access);
				InputStream in = client.get(url).getEntity().getContent();
				try {
					Reader reader = new InputStreamReader(in, "UTF-8");
					return info = new JSONObject(new JSONTokener(reader));
				} finally {
					in.close();
				}
			} catch (JSONException e) {
				throw new BadGateway(e);
			} catch (IOException e) {
				throw new BadGateway(e);
			}
		}

		public boolean isExpired(long now) {
			return now >= expires;
		}
	}

}
