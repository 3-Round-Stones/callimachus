package org.callimachusproject.server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLog implements HttpProcessor {
	private static final String REQUEST_QUEUE = AccessLog.class.getName()
			+ "#REQUEST_QUEUE";
	private static final String USERNAME = "username=";
	private static final String NIL = "-";
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|([^,\"]*)))?\\s*,?");
	private final Logger logger = LoggerFactory.getLogger(AccessLog.class);

	@Override
	public void process(HttpRequest req, HttpContext ctx) throws HttpException,
			IOException {
		if (!logger.isInfoEnabled())
			return;
		String addr = getRemoteAddress(ctx);
		boolean tracing = logger.isTraceEnabled();
		if (tracing || !NIL.endsWith(addr)) {
			setRequest(req, ctx);
		}
		if (!tracing)
			return;
		String username = getUsername(req, ctx).replaceAll("\\s+", "_");
		String line = req.getRequestLine().toString();
		logger.trace("{}\t{}\t\"{}\"", new Object[] { addr, username, line });
	}

	@Override
	public void process(HttpResponse resp, HttpContext ctx)
			throws HttpException, IOException {
		if (!logger.isInfoEnabled())
			return;
		String addr = getRemoteAddress(ctx);
		if (!logger.isTraceEnabled() && NIL.equals(addr))
			return;
		HttpRequest req = getRequest(ctx);
		if (req == null)
			return;
		String username = getUsername(req, ctx).replaceAll("\\s+", "_");
		String line = req.getRequestLine().toString();
		int code = resp.getStatusLine().getStatusCode();
		logger.info("{}\t{}\t\"{}\"\t{}", new Object[] { addr, username, line,
				code });
	}

	private String getRemoteAddress(HttpContext context) {
		if (context == null)
			return NIL;
		HttpInetConnection con = (HttpInetConnection) context
				.getAttribute(ExecutionContext.HTTP_CONNECTION);
		if (con == null)
			return NIL;
		return con.getRemoteAddress().getHostAddress();
	}

	private String getUsername(HttpRequest req, HttpContext ctx) {
		for (Header hd : req.getHeaders("Authorization")) {
			Matcher m = TOKENS_REGEX.matcher(hd.getValue());
			while (m.find()) {
				String key = m.group(1);
				if ("username".equals(key))
					return m.group(2);
			}
		}
		for (Header hd : req.getHeaders("Cookie")) {
			for (String cookie : hd.getValue().split("\\s*,\\s*")) {
				if (!cookie.contains(USERNAME))
					continue;
				String[] pair = cookie.split("\\s*;\\s*");
				for (String p : pair) {
					if (p.startsWith(USERNAME)) {
						return p.substring(USERNAME.length());
					}
				}
			}
		}
		return NIL;
	}

	private void setRequest(HttpRequest request, HttpContext context) {
		Queue<HttpRequest> queue = (Queue) context.getAttribute(REQUEST_QUEUE);
		if (queue == null) {
			context.setAttribute(REQUEST_QUEUE,
					queue = new LinkedList<HttpRequest>());
		}
		synchronized (queue) {
			queue.add(request);
		}
	}

	private HttpRequest getRequest(HttpContext context) {
		Queue<HttpRequest> queue = (Queue) context.getAttribute(REQUEST_QUEUE);
		if (queue == null)
			return null;
		synchronized (queue) {
			return queue.poll();
		}
	}

}
