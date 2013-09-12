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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.HttpUriClient;
import org.callimachusproject.engine.model.TermFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenIDAuthReader implements ParameterAuthReader {
	private final Logger logger = LoggerFactory.getLogger(OpenIDAuthReader.class);
	private final HttpUriClient client;
	private final TermFactory tf;
	private final String endpoint;
	private final String realm;

	public OpenIDAuthReader(String self, String endpoint, String realm, HttpUriClient client) {
		this.client = client;
		this.tf = TermFactory.newInstance(endpoint);
		this.endpoint = endpoint;
		this.realm = realm;
	}

	@Override
	public String getLoginPage(String returnTo, boolean loggedIn,
			String parameters, String[] via) {
		StringBuilder sb = new StringBuilder(endpoint);
		if (endpoint.indexOf('?') < 0) {
			sb.append('?');
		} else {
			sb.append('&');
		}
		sb.append("openid.ns=http://specs.openid.net/auth/2.0");
		if (parameters != null && parameters.contains("openid.mode=setup_needed")) {
			sb.append("&openid.mode=checkid_setup");
		} else if (loggedIn || parameters != null
				&& parameters.contains("openid.mode=id_res")) {
			sb.append("&openid.mode=checkid_immediate");
		} else {
			sb.append("&openid.mode=checkid_setup");
		}
		sb.append("&openid.return_to=").append(encode(returnTo));
		sb.append("&openid.realm=").append(encode(realm));
		sb.append("&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select");
		sb.append("&openid.identity=http://specs.openid.net/auth/2.0/identifier_select");
		sb.append("&openid.ns.ax=http://openid.net/srv/ax/1.0");
		sb.append("&openid.ax.mode=fetch_request");
		sb.append("&openid.ax.required=email,fullname,firstname,lastname");
		sb.append("&openid.ax.type.email=http://axschema.org/contact/email");
		sb.append("&openid.ax.type.fullname=http://axschema.org/namePerson");
		sb.append("&openid.ax.type.firstname=http://axschema.org/namePerson/first");
		sb.append("&openid.ax.type.lastname=http://axschema.org/namePerson/last");
		return sb.toString();
	}

	@Override
	public String getParameters(String method, String uri, String query, HttpEntity body) {
		if ("POST".equals(method)
				&& body != null
				&& body.getContentType() != null
				&& "application/x-www-form-urlencoded".equals(body
						.getContentType().getValue())) {
			try {
				return EntityUtils.toString(body);
			} catch (IOException e) {
				logger.warn(e.toString(), e);
			}
		}
		return query;
	}

	@Override
	public boolean isLoggingIn(String parameters) {
		return parameters.contains("openid.mode=id_res");
	}

	@Override
	public boolean isCanncelled(String parameters) {
		return parameters.contains("openid.mode=cancel");
	}

	@Override
	public boolean isValidParameters(String parameters, String[] via) throws IOException {
		String body = parameters.replace("openid.mode=id_res", "openid.mode=check_authentication");
		StringBuilder sb = new StringBuilder(endpoint);
		if (endpoint.indexOf('?') < 0) {
			sb.append('?');
		} else {
			sb.append('&');
		}
		sb.append(body);
		HttpEntity entity = client.getEntity(sb.toString(), "text/plain");
		try {
			return EntityUtils.toString(entity).contains("is_valid:true");
		} finally {
			EntityUtils.consume(entity);
		}
	}

	@Override
	public String getUserIdentifier(String parameters) {
		return resolve(getValueAfter(parameters, "&openid.claimed_id="));
	}

	@Override
	public String getUserFullName(String parameters) {
		Map<String, String> values = keyByName(parameters);
		Map<String, String> keys = keyByValue(parameters);
		String name = getAxValue("http://axschema.org/namePerson", keys, values);
		if (name != null)
			return name;
		String firstname = getAxValue("http://axschema.org/namePerson/first", keys, values);
		String lastname = getAxValue("http://axschema.org/namePerson/last", keys, values);
		String email = getAxValue("http://axschema.org/contact/email", keys, values);
		if (firstname != null && lastname != null) {
			return firstname + ' ' + lastname;
		} else if (email != null) {
			return email;
		} else {
			return getUserIdentifier(parameters);
		}
	}

	@Override
	public String getUserLogin(String parameters) {
		Map<String, String> values = keyByName(parameters);
		Map<String, String> keys = keyByValue(parameters);
		String email = getAxValue("http://axschema.org/contact/email", keys, values);
		if (email != null)
			return email;
		return getValueAfter(parameters, "&openid.claimed_id=");
	}

	private String resolve(String reference) {
		if (reference == null)
			return null;
		return tf.resolve(reference);
	}

	private String getAxValue(String type, Map<String, String> keys,
			Map<String, String> values) {
		String alias = keys.get(type);
		if (alias == null)
			return null;
		return values.get(alias.replace(".type.", ".value."));
	}

	private Map<String, String> keyByName(String cookie) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (String pair : cookie.split("&")) {
			int idx = pair.indexOf('=');
			if (idx > 0) {
				String name = decode(pair.substring(0, idx));
				String value = decode(pair.substring(idx + 1));
				map.put(name, value);
			}
		}
		return map;
	}

	private Map<String, String> keyByValue(String cookie) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (String pair : cookie.split("&")) {
			int idx = pair.indexOf('=');
			if (idx > 0) {
				String name = decode(pair.substring(0, idx));
				String value = decode(pair.substring(idx + 1));
				map.put(value, name);
			}
		}
		return map;
	}

	private String getValueAfter(String cookie, String token) {
		if (cookie == null)
			return null;
		int idx = cookie.indexOf(token);
		if (idx < 0)
			return null;
		int start = idx + token.length();
		int end = cookie.indexOf('&', start);
		if (end < 0) {
			end = cookie.length();
		}
		return decode(cookie.substring(start, end));
	}

}
