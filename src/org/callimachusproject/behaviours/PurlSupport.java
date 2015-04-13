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
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.rewrite.URITemplate;
import org.callimachusproject.traits.CalliObject;
import org.callimachusproject.util.PercentCodec;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Sparql;
import org.openrdf.http.object.client.HttpUriResponse;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.util.PathMatcher;
import org.openrdf.http.object.util.URLUtil;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PurlSupport implements CalliObject {
	private static final ContentType URI_LIST = ContentType.create("text/uri-list", "UTF-8");
	private static final EnglishReasonPhraseCatalog REASONS = EnglishReasonPhraseCatalog.INSTANCE;
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Pattern PURL_SYNTAX = Pattern
			.compile("(?:(?:(\\S+) )?(\\S+) )?([\\s\\S]+)");
	private static final Pattern HTTP_LINE = Pattern.compile("^(\\S+)");
	private static final Pattern HTTP_HEAD = Pattern
			.compile("(?<!\n)\r?\n(\\S+)\\s*:\\s*(.*)");
	private static final Pattern HTTP_BODY = Pattern
			.compile("\r?\n\r?\n([\\S\\s]+)");

	private final Logger logger = LoggerFactory.getLogger(PurlSupport.class);

	public HttpResponse createResponse(String method, String suffix)
			throws OpenRDFException, IOException {
		return createResponse(new BasicHttpRequest(method, suffix));
	}

	public HttpResponse createResponse(HttpRequest req)
			throws OpenRDFException, IOException {
		String method = req.getRequestLine().getMethod();
		String suffix = req.getRequestLine().getUri();
		TupleQueryResult rs = this.findPurlPatterns();
		try {
			while (rs.hasNext()) {
				BindingSet bs = rs.next();
				Value pattern = bs.getValue("pattern");
				Matcher m = PURL_SYNTAX.matcher(pattern.stringValue());
				if (!m.matches()) {
					logger.warn("Invalid PURL pattern: pattern");
					continue;
				}
				String matching = m.group(1);
				String regex = m.group(2);
				URITemplate template = new URITemplate(m.group(3));
				if (matching != null && !matching.equals(method)
						|| matching == null && !bs.hasBinding("status")
						&& bs.hasBinding("method")
						&& !bs.getValue("method").stringValue().equals(method))
					continue;
				Map<String, String[]> variables = match(suffix, regex);
				if (variables != null)
					return createResponse(bs, template.process(variables), req);
			}
			return null;
		} finally {
			rs.close();
		}
	}

	@Sparql("PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "SELECT ?method ?status ?phrase (str(?resource) AS ?pattern) {\n"
			+ "    {\n"
			+ "        $this calli:copy ?resource\n"
			+ "        BIND (\"GET\" AS ?method)\n"
			+ "    } UNION {\n"
			+ "        $this calli:post ?resource\n"
			+ "        BIND (\"POST\" AS ?method)\n"
			+ "    } UNION {\n"
			+ "        $this calli:put ?resource\n"
			+ "        BIND (\"PUT\" AS ?method)\n"
			+ "    } UNION {\n"
			+ "        $this calli:patch ?resource\n"
			+ "        BIND (\"PATCH\" AS ?method)\n"
			+ "    } UNION {\n"
			+ "        $this calli:delete ?resource\n"
			+ "        BIND (\"DELETE\" AS ?method)\n"
			+ "    } UNION {\n"
			+ "        $this calli:canonical ?resource\n"
			+ "        BIND (301 AS ?status)\n"
			+ "        BIND (\"Canonical\" AS ?phrase)\n"
			+ "    } UNION {\n"
			+ "        $this calli:alternate ?resource\n"
			+ "        BIND (302 AS ?status)\n"
			+ "        BIND (\"Alternate\" AS ?phrase)\n"
			+ "    } UNION {\n"
			+ "        $this calli:describedby ?resource\n"
			+ "        BIND (303 AS ?status)\n"
			+ "        BIND (\"Described by\" AS ?phrase)\n"
			+ "    } UNION {\n"
			+ "        $this calli:resides ?resource\n"
			+ "        BIND (307 AS ?status)\n"
			+ "        BIND (\"Resides\" AS ?phrase)\n"
			+ "    } UNION {\n"
			+ "        $this calli:moved ?resource\n"
			+ "        BIND (308 AS ?status)\n"
			+ "        BIND (\"Moved\" AS ?phrase)\n"
			+ "    } UNION {\n"
			+ "        $this calli:missing ?resource\n"
			+ "        BIND (\"GET\" AS ?method)\n"
			+ "        BIND (404 AS ?status)\n"
			+ "        BIND (\"Not found\" AS ?phrase)\n"
			+ "    } UNION {\n"
			+ "        $this calli:gone ?resource\n"
			+ "        BIND (\"GET\" AS ?method)\n"
			+ "        BIND (410 AS ?status)\n"
			+ "        BIND (\"Gone\" AS ?phrase)\n"
			+ "}    } ORDER BY ?status\n")
	protected abstract TupleQueryResult findPurlPatterns();

	private Map<String, String[]> match(String suffix, String regex) {
		if (regex == null && suffix.length() > 0 && suffix.charAt(0) != '?')
			return null;
		else if (regex == null)
			return asMap(suffix);
		String base = this.toString();
		CharSequence url = new StringBuilder(base).append(suffix);
		PathMatcher m = new PathMatcher(url, base.length());
		Map<String, String> variables = m.match(regex);
		if (variables == null)
			return null;
		Map<String, String[]> qs = asMap(suffix);
		for (Map.Entry<String, String> e : variables.entrySet()) {
			qs.put(e.getKey(), new String[] { e.getValue() });
		}
		return qs;
	}

	private Map<String, String[]> asMap(String uri) {
		Map<String, String[]> map = new LinkedHashMap<String, String[]>();
		int q = uri.indexOf('?');
		if (q >= 0) {
			String qs = uri.substring(q + 1);
			for (NameValuePair pair : URLEncodedUtils.parse(qs, UTF8)) {
				if (map.containsKey(pair.getName())) {
					String[] previous = map.get(pair.getName());
					String[] values = new String[previous.length + 1];
					System.arraycopy(previous, 0, values, 0, previous.length);
					values[previous.length] = pair.getValue();
					map.put(pair.getName(), values);
				} else {
					map.put(pair.getName(), new String[] { pair.getValue() });
				}
			}
		}
		map.put("this", new String[] { this.toString() });
		return map;
	}

	private HttpResponse createResponse(BindingSet bindings,
			CharSequence result, HttpRequest request) throws IOException,
			OpenRDFException {
		if (bindings.hasBinding("method")) {
			String method = bindings.getValue("method").stringValue();
			HttpUriRequest req = buildRequest(method, result, request);
			HttpUriResponse resp = executeRequest(req);
			if (!resp.containsHeader("Content-Location")) {
				resp.setHeader("Content-Location", resp.getSystemId());
			}
			if (bindings.hasBinding("status")) {
				HttpResponse r = newResponse(bindings);
				r.setHeaders(resp.getAllHeaders());
				r.setEntity(resp.getEntity());
				return r;
			}
			return resp;
		} else if (bindings.hasBinding("status")) {
			String loc = result.toString();
			String suffix = request.getRequestLine().getUri();
			int q = suffix.indexOf('?');
			String qs = q >= 0 ? suffix.substring(q) : "";
			String location = loc.indexOf('?') < 0 ? loc + qs : loc;
			HttpResponse resp = newResponse(bindings);
			resp.setHeader("Location", location);
			resp.setEntity(new StringEntity(location, URI_LIST));
			return resp;
		} else {
			return null;
		}
	}

	private HttpUriRequest buildRequest(final String method,
			CharSequence message, HttpRequest req) throws IOException {
		Matcher line = HTTP_LINE.matcher(message);
		if (!line.find())
			throw new IllegalArgumentException("Unsupported redirect syntax: "
					+ message);
		HttpEntityEnclosingRequestBase request = new HttpEntityEnclosingRequestBase() {
			public String getMethod() {
				return method;
			}
		};
		String loc = PercentCodec.encodeOthers(line.group(1));
		String suffix = req.getRequestLine().getUri();
		int q = suffix.indexOf('?');
		String qs = q >= 0 ? suffix.substring(q) : "";
		String location = loc.indexOf('?') < 0 ? loc + qs : loc;
		CharSequence target = URLUtil.resolve(location, this.toString());
		request.setURI(URI.create(target.toString()));
		request.setHeaders(req.getAllHeaders());
		Matcher body = HTTP_BODY.matcher(message);
		HttpEntity entity = getEntity(req);
		if (body.find()) {
			EntityUtils.consume(entity);
			request.setEntity(new StringEntity(body.group(1), "UTF-8"));
		} else if (entity != null) {
			request.setEntity(entity);
		}
		Matcher header = HTTP_HEAD.matcher(message);
		while (header.find()) {
			String name = header.group(1);
			String value = header.group(2);
			request.addHeader(name, value);
		}
		return request;
	}

	private HttpEntity getEntity(HttpRequest req) {
		if (req instanceof HttpEntityEnclosingRequest)
			return ((HttpEntityEnclosingRequest) req).getEntity();
		return null;
	}

	private HttpUriResponse executeRequest(HttpUriRequest request)
			throws ResponseException, IOException, OpenRDFException {
		return this.getHttpClient().getAnyResponse(request);
	}

	private HttpResponse newResponse(BindingSet bindings) {
		Literal status = (Literal) bindings.getValue("status");
		Value phrase = bindings.getValue("phrase");
		int sc = status.intValue();
		String msg = phrase == null ? REASONS.getReason(sc, null) : phrase
				.stringValue();
		return new BasicHttpResponse(HttpVersion.HTTP_1_1, sc, msg);
	}
}
