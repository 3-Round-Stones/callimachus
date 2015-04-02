/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.rewrite.URITemplate;
import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpUriResponse;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.fluid.Fluid;
import org.openrdf.http.object.fluid.FluidBuilder;
import org.openrdf.http.object.fluid.FluidException;
import org.openrdf.http.object.fluid.FluidFactory;
import org.openrdf.http.object.util.PathMatcher;

public abstract class PurlSupport implements CalliObject {
	private static final Pattern HTTP_LINE = Pattern
			.compile("^(?:(\\S+) +)?(\\S+)( +HTTP/\\d\\.\\d)?");
	private static final Pattern HTTP_HEAD = Pattern
			.compile("(?<!\n)\r?\n(\\S+)\\s*:\\s*(.*)");
	private static final Pattern HTTP_BODY = Pattern
			.compile("\r?\n\r?\n([\\S\\s]+)");

	public HttpUriRequest buildRequest(final String defaultMethod,
			String pattern, String suffix) throws IOException,
			FluidException {
		final CharSequence requestMessage = processUriTemplate(pattern, suffix);
		if (requestMessage == null)
			return null;
		Matcher line = HTTP_LINE.matcher(requestMessage);
		if (!line.find())
			throw new IllegalArgumentException("Unsupported redirect syntax: "
					+ requestMessage);
		final String method = line.group(1) == null ? defaultMethod : line
				.group(1);
		HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
			public String getMethod() {
				return method;
			}
		};
		String location = PercentCodec.encodeOthers(line.group(2),
				PercentCodec.ALLOWED);
		URI target = URI.create(TermFactory.newInstance(this.toString()).resolve(location));
		request.setURI(target.normalize());
		Matcher body = HTTP_BODY.matcher(requestMessage);
		StringEntity entity = null;
		if (body.find()) {
			entity = new StringEntity(body.group(1), "UTF-8");
			request.setEntity(entity);
		}
		Matcher header = HTTP_HEAD.matcher(requestMessage);
		while (header.find()) {
			String name = header.group(1);
			String value = header.group(2);
			request.addHeader(name, value);
			if (entity != null) {
				if ("Content-Encoding".equalsIgnoreCase(name)) {
					entity.setContentEncoding(value);
				} else if ("Content-Type".equalsIgnoreCase(name)) {
					entity.setContentType(value);
				}
			}
		}
		return request;
	}

	public HttpUriResponse executeRequest(HttpUriRequest request)
			throws ResponseException, IOException, OpenRDFException {
		return this.getHttpClient().getAnyResponse(request);
	}

	private CharSequence processUriTemplate(String cmd, String suffix)
			throws IOException, FluidException {
		Pattern pattern = compile(cmd);
		if (pattern == null && suffix.length() > 0 && suffix.charAt(0) != '?')
			return null; // don't auto match sub-paths
		String base = this.toString();
		PathMatcher m = new PathMatcher(base + suffix, base.length());
		Map<String, String> variables = pattern == null ? Collections
				.<String, String> emptyMap() : m.match(pattern);
		if (variables == null)
			return null; // not a match
		if (suffix.indexOf('?') >= 0) {
			String qs = suffix.substring(suffix.indexOf('?'));
			Map<String, ?> query = asMap(qs.substring(1));
			int size = variables.size() + query.size() + 1;
			Map<String, Object> vars = new HashMap<String, Object>(size);
			vars.putAll(query);
			vars.put("this", base);
			vars.putAll(variables);
			CharSequence result = template(cmd).process(vars);
			return appendQueryString(result, qs);
		} else if (!variables.isEmpty()) {
			int size = variables.size() + 1;
			Map<String, Object> vars = new HashMap<String, Object>(size);
			vars.put("this", base);
			vars.putAll(variables);
			return template(cmd).process(vars);
		} else {
			return template(cmd).process(Collections.singletonMap("this", base));
		}
	}

	public Pattern compile(String cmd) {
		int split = indexOfTemplate(cmd);
		if (split > 0) {
			String regex = cmd.substring(0, split - 1);
			try {
				return Pattern.compile(regex);
			} catch (PatternSyntaxException e) {
				return Pattern.compile(regex, Pattern.LITERAL);
			}
		} else {
			return null;
		}
	}

	public URITemplate template(String cmd) {
		return new URITemplate(cmd.substring(indexOfTemplate(cmd)));
	}

	public int indexOfTemplate(String command) {
		int end = command.indexOf('\n');
		if (end < 0) {
			end = command.length();
		}
		int split = command.indexOf(' ');
		if (split >= 0 && split < end) {
			return split + 1;
		} else {
			return 0;
		}
	}

	private Map<String, ?> asMap(String qs) throws IOException,
			FluidException {
		FluidFactory ff = FluidFactory.getInstance();
		FluidBuilder fb = ff.builder(getObjectConnection());
		Fluid fluid = fb.consume(qs, this.toString(), String.class,
				"application/x-www-form-urlencoded");
		return (Map<String, ?>) (Map) fluid.as(Map.class);
	}

	private CharSequence appendQueryString(CharSequence result, CharSequence qs) {
		if (result == null)
			return result;
		StringBuilder sb = new StringBuilder(result.length() + qs.length());
		sb.append(result);
		int split = sb.indexOf("\n");
		if (split < 0) {
			split = sb.length();
		}
		int q = sb.indexOf("?");
		if (0 <= q && q < split)
			return result;
		sb.insert(split, qs);
		return sb;
	}
}
