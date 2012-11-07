package org.callimachusproject.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.callimachusproject.client.HttpEntityWrapper;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.util.ChannelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLog extends Filter {
	private static final String USERNAME = "username=";
	private static final String NIL = "-";
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|([^,\"]*)))?\\s*,?");
	private final Logger logger = LoggerFactory.getLogger(AccessLog.class);

	public AccessLog(Filter delegate) {
		super(delegate);
	}

	@Override
	public HttpResponse intercept(Request req) throws IOException {
		if (logger.isTraceEnabled()) {
			String addr = req.getRemoteAddr().getHostAddress();
			String username = getUsername(req).replaceAll("\\s+", "_");
			String line = req.getRequestLine().toString();
			logger.trace("{}\t{}\t\"{}\"", new Object[] { addr, username, line });
		}
		return super.intercept(req);
	}

	@Override
	public HttpResponse filter(Request req, HttpResponse resp)
			throws IOException {
		resp = super.filter(req, resp);
		boolean tracing = logger.isTraceEnabled();
		if (!tracing && req.isInternal())
			return resp;
		final int code = resp.getStatusLine().getStatusCode();
		if (logger.isInfoEnabled() || logger.isWarnEnabled() && code >= 400
				|| logger.isErrorEnabled() && code >= 500) {
			final String addr = req.getRemoteAddr().getHostAddress();
			final String username = getUsername(req).replaceAll("\\s+",
					"_");
			final String line = req.getRequestLine().toString();
			if (tracing) {
				logger.trace("{}\t{}\t\"{}\"\t{}", new Object[] { addr,
						username, line, code });
			}
			HttpEntity entity = resp.getEntity();
			if (entity == null) {
				log(addr, username, line, code, 0);
			} else {
				resp.setEntity(new HttpEntityWrapper(entity) {
					@Override
					protected InputStream getDelegateContent()
							throws IOException {
						InputStream in = super.getDelegateContent();
						return logOnClose(addr, username, line, code, in);
					}
				});
			}
		}
		return resp;
	}

	InputStream logOnClose(final String addr, final String username,
			final String line, final int code, InputStream in) {
		final ReadableByteChannel delegate = ChannelUtil.newChannel(in);
		return ChannelUtil.newInputStream(new ReadableByteChannel() {
			private long length = 0;

			public boolean isOpen() {
				return delegate.isOpen();
			}

			public synchronized void close() throws IOException {
				delegate.close();
				log(addr, username, line, code, length);
			}

			public synchronized int read(ByteBuffer dst) throws IOException {
				int read = delegate.read(dst);
				length += read;
				return read;
			}
		});
	}

	void log(final String addr, final String username, final String line,
			final int code, long length) {
		if (code < 400) {
			logger.info("{}\t{}\t\"{}\"\t{}\t{}", new Object[] { addr,
					username, line, code, length });
		} else if (code < 500) {
			logger.warn("{}\t{}\t\"{}\"\t{}\t{}", new Object[] { addr,
					username, line, code, length });
		} else {
			logger.error("{}\t{}\t\"{}\"\t{}\t{}", new Object[] { addr,
					username, line, code, length });
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
						return p.substring(USERNAME.length());
					}
				}
			}
		}
		return NIL;
	}

}
