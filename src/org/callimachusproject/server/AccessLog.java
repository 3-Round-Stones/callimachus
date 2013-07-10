package org.callimachusproject.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.callimachusproject.client.StreamingHttpEntity;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLog implements AsyncExecChain {
	private static final String USERNAME = "username=";
	private static final String NIL = "-";
	private static final String FORENSIC_ATTR = AccessLog.class.getName() + "#forensicId";
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|([^,\"]*)))?\\s*,?");
	private final Logger logger = LoggerFactory.getLogger(AccessLog.class);
	private final String uid = "t"
			+ Long.toHexString(System.currentTimeMillis()) + "x";
	private final AtomicLong seq = new AtomicLong(0);
	private final AsyncExecChain delegate;

	public AccessLog(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target,
			final HttpRequest request, final HttpContext context,
			FutureCallback<HttpResponse> callback) {
		final String addr = getClientAddress(context);
		traceRequest(request, context, addr == null);
		return delegate.execute(target, request, context, new ResponseCallback(
				callback) {
			public void completed(HttpResponse result) {
				try {
					logResponse(request, context, result);
					super.completed(result);
				} catch (RuntimeException ex) {
					super.failed(ex);
				}
			}

			public void failed(Exception ex) {
				logCancel(addr, request);
				super.failed(ex);
			}

			public void cancelled() {
				logCancel(addr, request);
				super.cancelled();
			}
		});
	}

	void logResponse(HttpRequest req, HttpContext context, final HttpResponse resp) {
		final String addr = getClientAddress(context);
		traceResponse(req, context, resp, addr == null);
		if (addr == null)
			return;
		final int code = resp.getStatusLine().getStatusCode();
		if (logger.isInfoEnabled() || logger.isWarnEnabled() && code >= 400
				|| logger.isErrorEnabled() && code >= 500) {
			final String username = getUsername(req).replaceAll("\\s+",
					"_");
			final String line = req.getRequestLine().toString();
			final Header referer = req.getFirstHeader("Referer");
			final Header agent = req.getFirstHeader("User-Agent");
			HttpEntity entity = resp.getEntity();
			if (entity == null) {
				log(addr, username, line, code, 0, referer, agent);
			} else {
				final long length = entity.getContentLength();
				resp.setEntity(new StreamingHttpEntity(entity) {
					@Override
					protected InputStream getDelegateContent()
							throws IOException {
						InputStream in = super.getDelegateContent();
						return logOnClose(addr, username, line, code, length, referer, agent, in);
					}
				});
			}
		}
	}

	InputStream logOnClose(final String addr, final String username,
			final String line, final int code, final long length, final Header referer, final Header agent, InputStream in) {
		final ReadableByteChannel delegate = ChannelUtil.newChannel(in);
		return ChannelUtil.newInputStream(new ReadableByteChannel() {
			private long size = 0;
			private boolean complete;
			private boolean error;

			public boolean isOpen() {
				return delegate.isOpen();
			}

			public synchronized void close() throws IOException {
				delegate.close();
				if (!complete) {
					complete = true;
					if (error) {
						log(addr, username, line, 599, size, referer, agent);
					} else if (size < length) {
						log(addr, username, line, 499, size, referer, agent);
					} else {
						log(addr, username, line, code, size, referer, agent);
					}
				}
			}

			public synchronized int read(ByteBuffer dst) throws IOException {
				error = true;
				int read = delegate.read(dst);
				if (read < 0) {
					complete = true;
					log(addr, username, line, code, size, referer, agent);
				} else {
					size += read;
				}
				error = false;
				return read;
			}
		});
	}

	void logCancel(final String addr, final HttpRequest request) {
		String username = getUsername(request).replaceAll("\\s+", "_");
		String line = request.getRequestLine().toString();
		Header referer = request.getFirstHeader("Referer");
		Header agent = request.getFirstHeader("User-Agent");
		log(addr, username, line, 599, 0, referer, agent);
	}

	void log(String addr, String username, String line, int code, long length,
			Header referer, Header agent) {
		StringBuilder sb = new StringBuilder();
		sb.append(addr).append('\t').append(username);
		sb.append('\t').append('"').append(line).append('"');
		sb.append('\t').append(code).append('\t').append(length);
		if (referer == null) {
			sb.append('\t').append('-');
		} else {
			sb.append('\t').append('"').append(referer.getValue()).append('"');
		}
		if (agent == null) {
			sb.append('\t').append('-');
		} else {
			sb.append('\t').append('"').append(agent.getValue()).append('"');
		}
		if (code < 400 || code == 401) {
			logger.info(sb.toString());
		} else if (code < 500) {
			logger.warn(sb.toString());
		} else {
			logger.error(sb.toString());
		}
	}

	private String getUsername(HttpRequest req) {
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
						return decode(p.substring(USERNAME.length()));
					}
				}
			}
		}
		return NIL;
	}

	private void traceRequest(HttpRequest req, HttpContext context, boolean trace) {
		if (logger.isDebugEnabled() && !trace || logger.isTraceEnabled()) {
			String id = uid + seq.getAndIncrement();
			setForensicId(context, id);
			StringBuilder sb = new StringBuilder();
			sb.append("+").append(getForensicId(context));
			sb.append("|").append(req.getRequestLine().toString().replace('|', '_'));
			for (Header hd : req.getAllHeaders()) {
				sb.append("|").append(hd.getName().replace('|', '_'));
				sb.append(":").append(hd.getValue().replace('|', '_'));
			}
			if (trace) {
				logger.trace(sb.toString());
			} else {
				logger.debug(sb.toString());
			}
		}
	}

	private String getForensicId(HttpContext context) {
		return (String) context.getAttribute(FORENSIC_ATTR);
	}

	private void setForensicId(HttpContext context, String id) {
		context.setAttribute(FORENSIC_ATTR, id);
	}

	private void traceResponse(HttpRequest req, HttpContext context, HttpResponse resp, boolean trace) {
		if (logger.isDebugEnabled() && !trace || logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("-").append(getForensicId(context));
			sb.append("|").append(resp.getStatusLine().toString().replace('|', '_'));
			if (trace) {
				logger.trace(sb.toString());
			} else {
				logger.debug(sb.toString());
			}
		}
	}

	private String decode(String string) {
		try {
			return URLDecoder.decode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private String getClientAddress(HttpContext context) {
		if (context == null)
			return null;
		HttpConnection con = HttpCoreContext.adapt(context).getConnection();
		if (con instanceof HttpInetConnection)
			return ((HttpInetConnection) con).getRemoteAddress().getHostAddress();
		return null;
	}

}
