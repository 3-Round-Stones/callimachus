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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.rewrite.Substitution;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;

public abstract class PurlSupport implements CalliObject {
	private static final Pattern HTTP_LINE = Pattern
			.compile("^(?:(\\S+) +)?(\\S+)( +HTTP/\\d\\.\\d)?");
	private static final Pattern HTTP_HEAD = Pattern
			.compile("(?<!\n)\r?\n(\\S+)\\s*:\\s*(.*)");
	private static final Pattern HTTP_BODY = Pattern
			.compile("\r?\n\r?\n([\\S\\s]+)");

	public HttpUriRequest buildRequest(final String defaultMethod,
			String pattern, String queryString) throws IOException,
			FluidException {
		final CharSequence requestMessage = processUriTemplate(pattern,
				queryString);
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

	private CharSequence processUriTemplate(String pattern, String queryString)
			throws IOException, FluidException {
		String base = this.toString();
		FluidFactory ff = FluidFactory.getInstance();
		FluidBuilder fb = ff.builder(getObjectConnection());
		Fluid fluid = fb.consume(queryString, base, String.class,
				"application/x-www-form-urlencoded");
		Map<String, ?> map = (Map) fluid.as(Map.class,
				"application/x-www-form-urlencoded");
		if (queryString != null) {
			int size = base.length() + queryString.length() + 1;
			StringBuilder sb = new StringBuilder(size);
			sb.append(base).append('?').append(queryString);
			CharSequence qs = sb.subSequence(base.length(), sb.length());
			Substitution substitution = Substitution.compile(pattern);
			CharSequence result = substitution.replace(sb, map);
			return appendQueryString(result, qs);
		} else {
			Substitution substitution = Substitution.compile(pattern);
			return substitution.replace(base, map);
		}
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
		if (0 < q && q < split)
			return result;
		sb.insert(split, qs);
		return sb;
	}
}
