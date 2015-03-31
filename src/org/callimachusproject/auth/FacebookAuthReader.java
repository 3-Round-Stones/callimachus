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

import static org.callimachusproject.util.PercentCodec.decode;
import static org.callimachusproject.util.PercentCodec.encode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.http.object.exceptions.BadGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacebookAuthReader implements ParameterAuthReader {
	private static final String OAUTH_URL = "https://www.facebook.com/dialog/oauth";
	private static final String ACCESS_URL = "https://graph.facebook.com/oauth/access_token";
	private static final String ME_URL = "https://graph.facebook.com/me";

	private final Logger logger = LoggerFactory.getLogger(FacebookAuthReader.class);
	private final Map<String, AccessToken> tokens = new HashMap<String, AccessToken>();
	private final HttpUriClient client;
	private final String appId;
	private final String secret;

	public FacebookAuthReader(String self, String appId, CharSequence secret, HttpUriClient client) {
		this.client = client;
		this.appId = appId;
		this.secret = secret.toString();
	}

	@Override
	public String getLoginPage(String returnTo, boolean loggedIn,
			String parameters, String[] via) {
		StringBuilder sb = new StringBuilder();
		sb.append(OAUTH_URL);
		sb.append("?client_id=").append(encode(getFacebookAppId()));
		sb.append("&redirect_uri=").append(encode(returnTo));
		sb.append("&state=").append(encode(state(via)));
		sb.append("&scope=email");
		return sb.toString();
	}

	@Override
	public String getParameters(String method, String uri, String query, HttpEntity body) {
		return "redirect_uri=" + encode(uri) + '&' + query;
	}

	@Override
	public boolean isLoggingIn(String parameters) {
		return parameters.contains("state=");
	}

	@Override
	public boolean isCanncelled(String parameters) {
		return parameters.contains("error=");
	}

	@Override
	public boolean isValidParameters(String parameters, String[] via) throws IOException {
		return getAccessToken(parameters, via) != null;
	}

	@Override
	public String getUserIdentifier(String parameters) {
		AccessToken token = getAccessToken(parameters);
		if (token == null)
			return null;
		return token.getLink();
	}

	@Override
	public String getUserFullName(String parameters) {
		AccessToken token = getAccessToken(parameters);
		if (token == null)
			return null;
		return token.getName();
	}

	@Override
	public String getUserLogin(String parameters) {
		AccessToken token = getAccessToken(parameters);
		if (token == null)
			return null;
		return token.getLogin();
	}

	private String getFacebookAppId() {
		return appId;
	}

	private String getFacebookSecret() {
		return secret;
	}

	private AccessToken getAccessToken(String parameters) {
		return getAccessToken(parameters, null);
	}

	private AccessToken getAccessToken(String parameters, String[] via) {
		AccessToken token = null;
		synchronized (tokens) {
			token = tokens.get(parameters);
		}
		if (token == null && via != null) {
			token = verify(parameters, via);
		}
		if (token == null)
			return null;
		return token;
	}

	private AccessToken verify(String parameters, String[] via) {
		String code = getValueAfter(parameters, "code=");
		String error_description = getValueAfter(parameters, "error_description=");
		String state = getValueAfter(parameters, "state=");
		boolean verified = verifyState(state, via);
		if (error_description != null && error_description.length() > 0) {
			logger.warn("Facebook says: {}", error_description);
			return null;
		} else if (!verified) {
			logger.error("Invalid facebook manager state");
			return null;
		} else if (code == null) {
			logger.error("Could not login facebook user");
			return null;
		}
		long now = System.currentTimeMillis();
		synchronized (tokens) {
			Iterator<AccessToken> iter = tokens.values().iterator();
			while (iter.hasNext()) {
				AccessToken token = iter.next();
				if (token.isExpired(now)) {
					iter.remove();
				}
			}
			if (tokens.containsKey(parameters))
				return tokens.get(parameters);
		}
		String redirect_uri = getValueAfter(parameters, "redirect_uri=");
		String url = ACCESS_URL + "?client_id="
				+ encode(getFacebookAppId()) + "&redirect_uri="
				+ encode(redirect_uri) + "&client_secret="
				+ encode(getFacebookSecret()) + "&code=" + encode(code);
		try {
			HttpEntity entity = client.getEntity(url, "application/x-www-form-urlencoded");
			try {
				Scanner scanner = new Scanner(entity.getContent(), "UTF-8");
				String content = scanner.useDelimiter("\\A").next();
				AccessToken token = new AccessToken(now, content);
				synchronized (tokens) {
					tokens.put(parameters, token);
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

	private String state(String[] via) {
		StringBuilder sb = new StringBuilder();
		for (String v : via) {
			sb.append(v);
		}
		sb.append(secret);
		return DigestUtils.md5Hex(sb.toString());
	}

	private boolean verifyState(String decode, String[] via) {
		return decode != null && decode.equals(state(via));
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
				String url = ME_URL + "?access_token=" + encode(access);
				InputStream in = client.getEntity(url, "application/json").getContent();
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
