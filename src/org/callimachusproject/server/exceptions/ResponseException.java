/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.server.exceptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

/**
 * Base class for HTTP exceptions.
 */
public abstract class ResponseException extends RuntimeException {

	public static ResponseException create(HttpResponse resp) throws IOException {
		return create(resp, null);
	}

	public static ResponseException create(HttpResponse resp, String from) throws IOException {
		StatusLine line = resp.getStatusLine();
		int code = line.getStatusCode();
		String[] titleBody = readMessage(resp, line.getReasonPhrase());
		String msg = titleBody[0];
		String stack = titleBody[1];
		if (from != null && from.length() > 0) {
			if (stack.contains("\n")) {
				stack = stack.replaceFirst("\n", " from " + from + "\n");
			} else {
				stack += " from " + from;
			}
		}
		return create(code, msg, stack);
	}

	private static ResponseException create(int status, String msg, String stack) {
		switch (status) {
		case 400:
			return new BadRequest(msg, stack);
		case 403:
			return new Forbidden(msg, stack);
		case 404:
			return new NotFound(msg, stack);
		case 405:
			return new MethodNotAllowed(msg, stack);
		case 406:
			return new NotAcceptable(msg, stack);
		case 409:
			return new Conflict(msg, stack);
		case 410:
			return new Gone(msg, stack);
		case 415:
			return new UnsupportedMediaType(msg, stack);
		case 428:
			return new PreconditionRequired(msg, stack);
		case 429:
			return new TooManyRequests(msg, stack);
		case 500:
			return new InternalServerError(msg, stack);
		case 501:
			return new NotImplemented(msg, stack);
		case 503:
			return new ServiceUnavailable(msg, stack);
		case 504:
			return new GatewayTimeout(msg, stack);
		default:
			return new BadGateway(msg, stack);
		}
	}

	private static String[] readMessage(HttpResponse resp, String defaultTitle) throws IOException {
		HttpEntity entity = resp.getEntity();
		if (entity == null)
			return new String[]{defaultTitle, defaultTitle}; // no response
		try {
			StringWriter string = new StringWriter();
			InputStream in = entity.getContent();
			Header hd = resp.getFirstHeader("Content-Encoding");
			if (hd != null && "gzip".equals(hd.getValue())) {
				in = new GZIPInputStream(in);
			}
			InputStreamReader reader = new InputStreamReader(in, "UTF-8");
			try {
				int read;
				char[] cbuf = new char[1024];
				while ((read = reader.read(cbuf)) >= 0) {
					string.write(cbuf, 0, read);
				}
			} finally {
				reader.close();
			}
			String title = defaultTitle;
			String body = string.toString();
			if (body.startsWith("<")) {
				if (body.contains("<title") && body.contains("</title>")) {
					title = body.substring(body.indexOf("<title"), body.indexOf("</title>"));
					title = title.replaceAll("<title[^>]*>\\s*", "");
					title = decodeHtmlText(title).trim();
				} else if (body.contains("<TITLE") && body.contains("</TITLE>")) {
					title = body.substring(body.indexOf("<TITLE"), body.indexOf("</TITLE>"));
					title = title.replaceAll("<TITLE[^>]*>\\s*", "");
					title = decodeHtmlText(title).trim();
				} else {
					title = defaultTitle;
				}
				if (body.contains("<pre") && body.contains("</pre>")) {
					body = body.substring(body.indexOf("<pre"), body.indexOf("</pre>"));
					body = body.replaceAll("<pre[^>]*>", "");
					body = decodeHtmlText(body);
				} else if (body.contains("<PRE") && body.contains("</PRE>")) {
					body = body.substring(body.indexOf("<PRE"), body.indexOf("</PRE>"));
					body = body.replaceAll("<PRE[^>]*>", "");
					body = decodeHtmlText(body);
				} else {
					body = title;
				}
			}
			return new String[]{title, body.trim()};
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		} catch (IOException e) {
			return new String[]{defaultTitle, null};
		} finally {
			EntityUtils.consume(entity);
		}
	}

	private static String decodeHtmlText(String body) {
		body = body.replaceAll("<[^>]*>", "\n");
		body = body.replaceAll("\n+", "\n");
		body = body.replaceAll("&lt;", "<");
		body = body.replaceAll("&gt;", ">");
		body = body.replaceAll("&nbsp;", " ");
		body = body.replaceAll("&amp;", "&");
		return body;
	}

	private static final long serialVersionUID = -4156041448577237448L;
	private String msg;
	private String full;

	public ResponseException(String message) {
		super(message);
		this.msg = trimMessage(full = firstMessage(message));
	}

	public ResponseException(String message, Throwable cause) {
		super(message, cause);
		this.msg = trimMessage(full = firstMessage(message));
	}

	public ResponseException(Throwable cause) {
		super(cause);
		if (cause instanceof ResponseException) {
			this.msg = trimMessage(full = firstMessage(((ResponseException) cause).getLongMessage()));
		} else {
			this.msg = trimMessage(full = firstMessage(cause.toString()));
		}
	}

	public ResponseException(String message, String stack) {
		super(stack == null ? message : stack);
		this.msg = trimMessage(full = firstMessage(message));
	}

	public abstract int getStatusCode();

	public Header[] getResponseHeaders() {
		return new Header[0];
	}

	@Override
	public String toString() {
		return getDetailMessage();
	}

	public String getShortMessage() {
		return msg;
	}

	public String getLongMessage() {
		return full;
	}

	public String getDetailMessage() {
		return super.getMessage();
	}

	private String firstMessage(String msg) {
		if (msg == null) {
			Throwable cause = getCause();
			if (cause == null) {
				msg = getClass().getName();
			} else {
				msg = cause.getClass().getName();
			}
		}
		if (msg.contains("\r\n\tat ")) {
			msg = msg.substring(0, msg.indexOf("\r\n\tat "));
		}
		if (msg.contains("\n\tat ")) {
			msg = msg.substring(0, msg.indexOf("\n\tat "));
		}
		return trimExceptionClass(msg, this);
	}

	private String trimMessage(String msg) {
		if (msg.contains("\r")) {
			msg = msg.substring(0, msg.indexOf("\r"));
		}
		if (msg.contains("\n")) {
			msg = msg.substring(0, msg.indexOf("\n"));
		}
		if (msg.length() > 192) {
			msg = msg.substring(0, 136) + "..." + msg.substring(msg.length() - 53);
		}
		return msg;
	}

	private String trimExceptionClass(String msg, Throwable cause) {
		if (cause == null)
			return msg;
		String prefix = cause.getClass().getName() + ": ";
		if (msg.startsWith(prefix)) {
			msg = msg.substring(prefix.length());
		}
		return trimExceptionClass(msg, cause.getCause());
	}

}
