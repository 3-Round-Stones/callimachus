/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * Copyright 2012-2013 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.server.helpers;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.server.exceptions.ResponseException;
import org.openrdf.repository.object.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds an HTTP response.
 * 
 * @author James Leigh
 */
public class ResponseBuilder {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final StatusLine _200 = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
	private static final StatusLine _204 = new BasicStatusLine(HttpVersion.HTTP_1_1, 204, "No Content");
	private static final StatusLine _302 = new BasicStatusLine(HttpVersion.HTTP_1_1, 302, "Found");
	private static final StatusLine _303 = new BasicStatusLine(HttpVersion.HTTP_1_1, 303, "See Other");
	private static final StatusLine _304 = new BasicStatusLine(HttpVersion.HTTP_1_1, 304, "Not Modified");
	private static final StatusLine _404 = new BasicStatusLine(HttpVersion.HTTP_1_1, 404, "Not Found");
	private static final StatusLine _412 = new BasicStatusLine(HttpVersion.HTTP_1_1, 412, "Precondition Failed");
	static final StatusLine _500 = new BasicStatusLine(HttpVersion.HTTP_1_1, 500, "Internal Server Error");
	private static final Pattern URL_PATTERN = Pattern
			.compile("\\w+://(?:\\.?[^\\s}>\\)\\]\\.])+");

	private final Logger logger = LoggerFactory.getLogger(ResponseBuilder.class);
	private final AuthorizationService service = AuthorizationService.getInstance();
	private final String systemId;
	private final ObjectRepository repository;

	public ResponseBuilder(HttpRequest request, HttpContext context) {
		ResourceTransaction trans = CalliContext.adapt(context).getResourceTransaction();
		if (trans == null) {
			this.systemId = new Request(request).getRequestURL();
			this.repository = null;
		} else {
			this.systemId = trans.getRequestURL();
			this.repository = trans.getObjectConnection().getRepository();
		}
	}

	public ResponseBuilder(ResourceTransaction request) {
		this.systemId = request.getRequestURL();
		this.repository = request.getObjectConnection().getRepository();
	}

	public ResponseBuilder(Request request) {
		this(request.getRequestURL());
	}

	public ResponseBuilder(String systemId) {
		this.systemId = systemId;
		this.repository = null;
	}

	public HttpUriResponse ok(HttpEntity entity) {
		BasicHttpResponse response = new BasicHttpResponse(_200);
		response.setEntity(entity);
		return respond(response);
	}

	public HttpUriResponse noContent() {
		BasicHttpResponse response = new BasicHttpResponse(_204);
		response.setHeader("Content-Length", "0");
		return respond(response);
	}

	public HttpUriResponse found(String location) {
		BasicHttpResponse response = new BasicHttpResponse(_302);
		response.addHeader("Location", location);
		response.setEntity(new StringEntity(location, ContentType.create("text/uri-list", UTF8)));
		return respond(response);
	}

	public HttpUriResponse see(String location) {
		BasicHttpResponse response = new BasicHttpResponse(_303);
		response.addHeader("Location", location);
		response.setEntity(new StringEntity(location, ContentType.create("text/uri-list", UTF8)));
		return respond(response);
	}

	public HttpUriResponse notModified() {
		BasicHttpResponse response = new BasicHttpResponse(_304);
		response.setHeader("Content-Length", "0");
		return respond(response);
	}

	public HttpUriResponse badRequest(String message) {
		BasicStatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, 400, message);
		HttpResponse response = new EntityRemovedHttpResponse(status);
		response.setHeader("Content-Type", "text/html;charset=UTF-8");
		byte[] body = formatPage(createPage(message));
		response.setHeader("Content-Length", String.valueOf(body.length));
		ReadableByteChannel in = ChannelUtil.newChannel(body);
		response.setEntity(new ReadableHttpEntityChannel("text/html;charset=UTF-8", body.length, in));
		return respond(response);
	}

	public HttpUriResponse notFound() {
		HttpResponse response = new EntityRemovedHttpResponse(_404);
		response.setHeader("Content-Type", "text/html;charset=UTF-8");
		byte[] body = formatPage(createPage("Not Found"));
		response.setHeader("Content-Length", String.valueOf(body.length));
		ReadableByteChannel in = ChannelUtil.newChannel(body);
		response.setEntity(new ReadableHttpEntityChannel("text/html;charset=UTF-8", body.length, in));
		return respond(response);
	}

	public HttpUriResponse preconditionFailed() {
		BasicHttpResponse response = new BasicHttpResponse(_412);
		response.setHeader("Content-Length", "0");
		return respond(response);
	}

	public HttpUriResponse preconditionFailed(String phrase) {
		BasicStatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, 412, phrase);
		BasicHttpResponse response = new BasicHttpResponse(status);
		response.setHeader("Content-Length", "0");
		return respond(response);
	}

	public HttpUriResponse serverError() {
		BasicHttpResponse response = new BasicHttpResponse(_500);
		response.setHeader("Content-Type", "text/html;charset=UTF-8");
		byte[] body = formatPage(createPage("Internal Server Error"));
		response.setHeader("Content-Length", String.valueOf(body.length));
		ReadableByteChannel in = ChannelUtil.newChannel(body);
		response.setEntity(new ReadableHttpEntityChannel("text/html;charset=UTF-8", body.length, in));
		return respond(response);
	}

	public HttpUriResponse noContent(int code, String phrase) {
		BasicStatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, code, phrase);
		BasicHttpResponse response = new BasicHttpResponse(status);
		response.setHeader("Content-Length", "0");
		return respond(response);
	}

	public HttpUriResponse content(int code, String phrase, HttpEntity entity) {
		BasicStatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, code, phrase);
		BasicHttpResponse response = new BasicHttpResponse(status);
		response.setEntity(entity);
		return respond(response);
	}

	public HttpUriResponse respond(HttpResponse response) {
		if (response instanceof HttpUriResponse)
			return (HttpUriResponse) response;
		return new HttpUriResponse(systemId, response);
	}

	public HttpUriResponse exception(ResponseException exception) {
		ProtocolVersion ver = HttpVersion.HTTP_1_1;
		int code = exception.getStatusCode();
		String phrase = exception.getShortMessage();
		HttpResponse response = new EntityRemovedHttpResponse(ver, code, phrase);
		String type = "text/html;charset=UTF-8";
		response.setHeader("Content-Type", type);
		byte[] body = formatPage(createErrorPage(exception));
		response.setHeader("Content-Length", String.valueOf(body.length));
		ReadableByteChannel in = ChannelUtil.newChannel(body);
		HttpEntity entity = new ReadableHttpEntityChannel(type, body.length, in);
		response.setEntity(entity);
		return respond(response);
	}

	private String createErrorPage(ResponseException exception) {
		Writer writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer);
		try {
			print.append("<html xmlns='http://www.w3.org/1999/xhtml'>\n");
			print.append("<head><title>");
			print.append(enc(exception.getLongMessage()));
			print.append("</title></head>\n");
			print.append("<body>\n");
			print.append("<h1>");
			print.append(html(exception.getLongMessage()));
			print.append("</h1>\n");
			if (exception.getCause() != null) {
				print.append("<pre>");
				Writer sw = new StringWriter();
				PrintWriter print1 = new PrintWriter(sw);
				exception.printStackTrace(print1);
				print1.close();
				print.append(enc(sw.toString()));
				print.append("</pre>\n");
			} else if (exception.getStatusCode() > 500) {
				print.append("<pre>");
				print.append(enc(exception.getDetailMessage()));
				print.append("</pre>\n");
			}
			print.append("</body>\n");
			print.append("</html>\n");
		} finally {
			print.close();
		}
		return writer.toString();
	}

	private String createPage(String title) {
		Writer writer = new StringWriter();
		PrintWriter print = new PrintWriter(writer);
		try {
			print.append("<html xmlns='http://www.w3.org/1999/xhtml'>\n");
			print.append("<head><title>");
			print.append(enc(title));
			print.append("</title></head>\n");
			print.append("<body>\n");
			print.append("<h1>");
			print.append(html(title));
			print.append("</h1>\n");
			print.append("</body>\n");
			print.append("</html>\n");
		} finally {
			print.close();
		}
		return writer.toString();
	}

	private String html(String string) {
		if (string.contains("://")) {
			int end = 0;
			StringBuilder sb = new StringBuilder();
			Matcher m = URL_PATTERN.matcher(string);
			while (m.find()) {
				String url = m.group();
				sb.append(enc(string.substring(end, m.start())));
				sb.append("<a href='").append(enc(url)).append("'>");
				int path = url.indexOf('/', url.indexOf("://") + 3);
				String label = path > 0 ? url.substring(path) : url;
				sb.append(enc(label));
				sb.append("</a>");
				end = m.end();
			}
			sb.append(enc(string.substring(end, string.length())));
			return sb.toString();
		} else {
			return enc(string);
		}
	}

	private String enc(String string) {
		String result = string.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		result = result.replace(">", "&gt;");
		return result;
	}

	private byte[] formatPage(String body) {
		if (repository == null)
			return body.getBytes(UTF8);
		try {
			AuthorizationManager manager = service.get(repository);
			if (manager == null)
				return body.getBytes(UTF8);
			DetachedRealm realm = manager.getRealm(systemId);
			if (realm == null)
				return body.getBytes(UTF8);
			ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
			OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
			int q = systemId.indexOf('?');
			String query = q > 0 ? systemId.substring(q) : null;
			realm.transformErrorPage(body, w, systemId, query);
			w.close();
			return out.toByteArray();
		} catch (Exception exc) {
			logger.error("Could not creating error page: " + systemId, exc);
			return body.getBytes(UTF8);
		}
	}

}
