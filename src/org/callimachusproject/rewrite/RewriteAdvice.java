package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.type;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.GatewayTimeout;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class RewriteAdvice implements Advice {
	private static final String Q;
	private static final String A;
	private static final String H;
	static {
		try {
			Q = URLEncoder.encode("?", "UTF-8");
			A = URLEncoder.encode("&", "UTF-8");
			H = URLEncoder.encode("#", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private final Logger logger = LoggerFactory.getLogger(RewriteAdvice.class);
	private final Pattern[] absolutes;
	private final Pattern[] paths;
	private final String[] bindingNames;
	private final String template;
	private final boolean dash;
	/** escape non-alphanumeric characters before applying the transformation */
	private boolean B;
	/** match in a case-insensitive manner */
	private boolean nocase;
	/**
	 * Do not encode special characters, such as & and ?, for example, to their
	 * hexcode equivalent in location
	 */
	private boolean noescape;
	/** causes a HTTP redirect to be issued to the browser */
	private int code = 302;
	private String phrase = "Found";
	/** set a cookie when a particular */
	private final List<String> cookies = new ArrayList<String>();
	private String contentType;

	public RewriteAdvice(String[] patterns, String[] bindingNames,
			String location, String[] flags) {
		readFlags(flags);
		this.absolutes = compile(patterns, false);
		this.paths = compile(patterns, true);
		this.bindingNames = bindingNames;
		this.template = location;
		this.dash = "-".equals(location);
	}

	public Object intercept(ObjectMessage message) throws Exception {
		Map<String, String> variables = getVariables(message.getParameters());
		Object target = message.getTarget();
		String location = location(target.toString(), variables);
		Method method = message.getMethod();
		ObjectConnection con = null;
		if (target instanceof RDFObject) {
			con = ((RDFObject) target).getObjectConnection();
		}
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		HttpResponse resp = response(method, message.getParameters(),
				target.toString(), location, fb);
		for (String cookie : cookies) {
			resp.addHeader("Set-Cookie", cookie);
		}
		if (contentType != null) {
			resp.setHeader("Content-Type", contentType);
		}
		Fluid fluid = fb.consume(resp, target.toString(), HttpResponse.class,
				"message/http");
		type mediaType = method.getAnnotation(type.class);
		if (mediaType == null)
			return fluid.as(method.getGenericReturnType(), "message/http");
		return fluid.as(method.getGenericReturnType(), mediaType.value());
	}

	private void readFlags(String[] flags) {
		for (String flag : flags) {
			try {
				if (flag.contains("\n") || flag.contains("\r")) {
					logger.warn("Illegal rewrite flag: {}", flag);
				} else {
					readFlag(flag);
				}
			} catch (Exception e) {
				logger.warn("Illegal rewrite flag: {}", flag);
			}
		}
	}

	private void readFlag(String flag) {
		if ("B".equals(flag)) {
			B = true;
		} else if ("NC".equals(flag) || "nocase".equals(flag)) {
			nocase = true;
		} else if ("NE".equals(flag) || "noescape".equals(flag)) {
			noescape = true;
		} else if ("P".equals(flag) || "proxy".equals(flag)) {
			code = 0;
			phrase = null;
		} else if ("F".equals(flag) || "forbidden".equals(flag)) {
			code = 403;
			phrase = "Forbidden";
		} else if ("G".equals(flag) || "gone".equals(flag)) {
			code = 410;
			phrase = "Gone";
		} else if ("R".equals(flag) || "redirect".equals(flag)) {
			code = 302;
			phrase = "Found";
		} else if (flag.startsWith("R=") || flag.startsWith("redirect=")) {
			String expect = flag.substring(flag.indexOf('=') + 1);
			String[] values = expect.split("[\\s\\-]+");
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < values.length; i++) {
				sb.append(values[i].substring(0, 1).toUpperCase());
				sb.append(values[i].substring(1));
				if (i < values.length - 1) {
					sb.append(" ");
				}
			}
			code = Integer.parseInt(values[0]);
			if (sb.length() > 1) {
				phrase = sb.toString();
			} else {
				phrase = values[0];
			}
		} else if (flag.startsWith("T=") || flag.startsWith("type=")) {
			contentType = flag.substring(flag.indexOf('=') + 1);
		} else if (flag.startsWith("CO=") || flag.startsWith("cookie=")) {
			String[] fields = flag.substring(flag.indexOf('=') + 1).split(":");
			StringBuilder cookie = new StringBuilder();
			cookie.append(fields[0]).append("=").append(fields[1]);
			if (fields.length > 2) {
				cookie.append("; Domain=").append(fields[2]);
			}
			if (fields.length > 3) {
				cookie.append("; Max-Age=").append(
						Integer.parseInt(fields[3]) * 60);
			}
			if (fields.length > 4) {
				if ("true".equalsIgnoreCase(fields[4]) || "1".equals(fields[4])
						|| "secure".equalsIgnoreCase(fields[4])) {
					cookie.append("; Secure");
				}
			}
			if (fields.length > 5) {
				if ("true".equalsIgnoreCase(fields[5]) || "1".equals(fields[5])
						|| "HttpOnly".equalsIgnoreCase(fields[5])) {
					cookie.append("; HttpOnly");
				}
			}
			cookies.add(cookie.toString());
		}
	}

	private Pattern[] compile(String[] patterns, boolean path) {
		if (patterns == null || patterns.length == 0)
			return new Pattern[0];
		int flag = 0;
		if (nocase) {
			flag = Pattern.CASE_INSENSITIVE;
		}
		List<Pattern> result = new ArrayList<Pattern>(patterns.length);
		for (int i = 0; i < patterns.length; i++) {
			if (path == patterns[i].startsWith("^/")) {
				result.add(Pattern.compile(patterns[i], flag));
			}
		}
		return result.toArray(new Pattern[result.size()]);
	}

	private Map<String, String> getVariables(Object[] parameters) {
		if (bindingNames == null || bindingNames.length == 0)
			return Collections.emptyMap();
		Map<String, String> map = new HashMap<String, String>(
				bindingNames.length);
		for (int i = 0; i < bindingNames.length; i++) {
			String key = bindingNames[i];
			if (key != null) {
				Object param = parameters[i];
				if (param != null) {
					map.put(key, param.toString());
				}
			}
		}
		return map;
	}

	private String location(String target, Map<String, String> variables)
			throws UnsupportedEncodingException {
		if (dash)
			return target;
		URI uri = URI.create(target);
		Matcher matcher = match(uri);
		String decoded = apply(matcher, template, variables);
		String path = encode(decoded);
		String url = URI.create(path).toASCIIString();
		return TermFactory.newInstance(target).reference(url).stringValue();
	}

	private Matcher match(URI uri) {
		if (absolutes.length > 0) {
			String decoded = decode(uri);
			for (Pattern pattern : absolutes) {
				Matcher m = pattern.matcher(decoded);
				if (m.find())
					return m;
			}
		} else if (paths.length > 0) {
			String decoded = uri.getPath();
			for (Pattern pattern : paths) {
				Matcher m = pattern.matcher(decoded);
				if (m.find())
					return m;
			}
		}
		return null;
	}

	private String decode(URI uri) {
		StringBuffer sb = new StringBuffer();
		sb.append(uri.getScheme());
		sb.append(':');
		if (uri.isOpaque()) {
			sb.append(uri.getSchemeSpecificPart());
		} else {
			sb.append("//");
			sb.append(uri.getAuthority());
			sb.append(uri.getPath());
		}
		return sb.toString();
	}

	private String apply(Matcher m, String template,
			Map<String, String> variables) throws UnsupportedEncodingException {
		int dollar = template.indexOf('$');
		int percent = template.indexOf('%');
		if (dollar < 0 && percent < 0)
			return template;
		StringBuilder sb = new StringBuilder(255);
		for (int i = 0, n = template.length(); i < n; i++) {
			char chr = template.charAt(i);
			if (chr == '$' && i + 1 < n) {
				char next = template.charAt(++i);
				if (next == '$') {
					sb.append(next);
				} else if (next >= '0' && next <= '9' && m != null) {
					int idx = next - '0';
					try {
						sb.append(inline(m.group(idx)));
					} catch (IndexOutOfBoundsException e) {
						sb.append(chr).append(next);
					}
				} else {
					sb.append(chr).append(next);
				}
			} else if (chr == '%' && i + 3 < n) {
				char next = template.charAt(++i);
				if (next == '%') {
					sb.append(next);
				} else if (next == '{') {
					int j = template.indexOf('}', i + 1);
					if (j > i) {
						String name = template.substring(i + 1, j);
						String value = variables.get(name);
						if (value != null) {
							sb.append(inline(value));
						}
						i = j;
					} else {
						sb.append(chr).append(next);
					}
				} else {
					sb.append(chr).append(next);
				}
			} else {
				sb.append(chr);
			}
		}
		return sb.toString();
	}

	private CharSequence inline(String value)
			throws UnsupportedEncodingException {
		if (B) {
			return URLEncoder.encode(value, "UTF-8");
		} else {
			return value;
		}
	}

	private String encode(String decoded) {
		if (noescape)
			return decoded;
		return decoded.replace("?", Q).replace("&", A).replace("#", H);
	}

	private HttpResponse response(Method method, Object[] parameters,
			String base, String location, FluidBuilder fb)
			throws GatewayTimeout, IOException,
			TransformerConfigurationException, OpenRDFException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerException {
		if (code == 0) {
			HttpRequest req = createRequest(method, parameters, base, location,
					fb);
			HttpResponse resp = HTTPObjectClient.getInstance().service(req);
			if (contentType != null) {
				resp.addHeader("Accept", contentType);
			}
			resp.addHeader("Content-Location", location);
			return resp;
		}
		HttpResponse resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, code,
				phrase);
		resp.setHeader("Location", location);
		return resp;
	}

	private HttpRequest createRequest(Method method, Object[] parameters,
			String base, String location, FluidBuilder fb)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			UnsupportedEncodingException {
		String req_method = getMethod(method);
		HttpRequest req = new BasicHttpRequest(req_method, location);
		Type[] ptypes = method.getGenericParameterTypes();
		Annotation[][] anns = method.getParameterAnnotations();
		loop: for (int i = 0; i < anns.length; i++) {
			String[] media = new String[0];
			for (Annotation ann : anns[i]) {
				if (ann instanceof type) {
					media = ((type) ann).value();
				}
			}
			Fluid fluid = fb.consume(parameters[i], base, ptypes[i], media);
			for (Annotation ann : anns[i]) {
				if (ann instanceof header) {
					String value = fluid.asString("text/plain");
					for (String name : ((header) ann).value()) {
						req.addHeader(name, value);
					}
					continue loop;
				} else if (ann instanceof query
						&& ((query) ann).value().length == 1
						&& "*".equals(((query) ann).value()[0])) {
					String value = fluid
							.asString("application/x-www-form-urlencoded");
					req = appendQueryString(req, value);
					continue loop;
				} else if (ann instanceof query) {
					String value = fluid.asString("text/plain");
					String encoded = "";
					if (value != null) {
						encoded = "=" + URLEncoder.encode(value, "UTF_8");
					}
					for (String name : ((query) ann).value()) {
						req = appendQueryString(req,
								URLEncoder.encode(name, "UTF-8") + encoded);
					}
					continue loop;
				}
			}
			if (media.length > 0) {
				HttpEntity entity = fluid.asHttpEntity(media);
				req = replaceHttpEntity(req, entity);
				req.setHeader("Content-Type", fluid.toHttpEntityMedia(media));
			} else {
				fluid.asVoid();
			}
		}
		return req;
	}

	private String getMethod(Method method) {
		method ann = method.getAnnotation(method.class);
		if (ann == null || ann.value().length < 1)
			return "GET";
		String[] values = ann.value();
		if ("HEAD".equals(values[0]) && Arrays.asList(values).contains("GET"))
			return "GET";
		return values[0];
	}

	private HttpRequest appendQueryString(HttpRequest req, String value) {
		String uri = req.getRequestLine().getUri();
		if (uri.contains("?")) {
			uri = uri + "&" + value;
		} else {
			uri = uri + "?" + value;
		}
		if (req instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest result = new BasicHttpEntityEnclosingRequest(
					req.getRequestLine().getMethod(), uri);
			for (Header hd : req.getAllHeaders()) {
				result.addHeader(hd);
			}
			result.setEntity(((HttpEntityEnclosingRequest) req).getEntity());
			return result;
		}
		HttpRequest result = new BasicHttpRequest(req.getRequestLine()
				.getMethod(), uri);
		for (Header hd : req.getAllHeaders()) {
			result.addHeader(hd);
		}
		return result;
	}

	private HttpRequest replaceHttpEntity(HttpRequest req, HttpEntity entity)
			throws IOException {
		HttpEntityEnclosingRequest result = new BasicHttpEntityEnclosingRequest(
				req.getRequestLine());
		for (Header hd : req.getAllHeaders()) {
			result.addHeader(hd);
		}
		result.setEntity(entity);
		if (req instanceof HttpEntityEnclosingRequest) {
			EntityUtils.consume(((HttpEntityEnclosingRequest) req).getEntity());
		}
		return result;
	}

}
