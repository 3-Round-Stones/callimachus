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
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.rewrite.Substitution;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.traits.CalliObject;
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
		final String requestMessage = processUriTemplate(pattern, queryString);
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
		String location = line.group(2);
		URI target = URI.create(this.toString()).resolve(location);
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

	private String processUriTemplate(String pattern, String queryString)
			throws IOException, FluidException {
		String base = this.toString();
		FluidFactory ff = FluidFactory.getInstance();
		FluidBuilder fb = ff.builder(getObjectConnection());
		Fluid fluid = fb.consume(queryString, base, String.class,
				"application/x-www-form-urlencoded");
		Map<String, ?> map = (Map) fluid.as(Map.class,
				"application/x-www-form-urlencoded");
		Substitution substitution = Substitution.compile(pattern);
		String result = substitution.replace(base, map);
		if (result == null && queryString != null) {
			result = substitution.replace(base + "?" + queryString, map);
		}
		if (result != null && result.length() > 0) {
			int split = result.indexOf("\n");
			String location = result;
			if (split >= 0) {
				location = result.substring(0, split);
			}
			int size = result.length();
			if (queryString != null) {
				size += queryString.length() + 1;
			}
			StringBuilder sb = new StringBuilder(size);
			sb.append(location);
			if (queryString != null && location.indexOf('?') < 0) {
				sb.append('?').append(queryString);
			}
			if (split >= 0) {
				sb.append(result.substring(split));
			}
			return sb.toString();
		} else {
			return null;
		}
	}
}
