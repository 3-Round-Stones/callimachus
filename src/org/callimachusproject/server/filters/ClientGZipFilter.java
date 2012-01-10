/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.filters;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;

/**
 * Compresses the request and uncompresses the response.
 */
public class ClientGZipFilter extends Filter {
	private static String hostname;
	static {
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "AliBaba";
		}
	}
	private static String WARN_214 = "214 " + hostname
			+ " \"Transformation applied\"";

	public ClientGZipFilter(Filter delegate) {
		super(delegate);
	}

	@Override
	public HttpResponse intercept(Request request) throws IOException {
		acceptEncoding(request);
		return removeEncoding(request, super.intercept(request));
	}

	public Request filter(Request req) throws IOException {
		acceptEncoding(req);
		return super.filter(req);
	}

	public HttpResponse filter(Request req, HttpResponse resp) throws IOException {
		resp = super.filter(req, resp);
		return removeEncoding(req, resp);
	}

	private void acceptEncoding(Request req) {
		if (!req.containsHeader("Accept-Encoding")) {
			req.setHeader("Accept-Encoding", "gzip");
		}
		HttpEntity entity = req.getEntity();
		if (entity == null)
			return;
		Header cache = req.getFirstHeader("Cache-Control");
		if (cache != null && cache.getValue().contains("no-transform"))
			return;
		long length = getLength(req.getFirstHeader("Content-Length"), -1);
		length = getLength(entity, length);
		boolean big = length < 0 || length > 500;
		if (!req.containsHeader("Content-Encoding") && big && isCompressable(req)) {
			req.removeHeaders("Content-MD5");
			req.removeHeaders("Content-Length");
			req.setHeader("Content-Encoding", "gzip");
			req.addHeader("Warning", WARN_214);
			if (entity instanceof GUnzipEntity) {
				req.setEntity(((GUnzipEntity) entity).getEntityDelegate());
			} else {
				req.setEntity(new GZipEntity(entity));
			}
		}
	}

	private HttpResponse removeEncoding(Request req, HttpResponse resp) {
		if (resp == null)
			return resp;
		HttpEntity entity = resp.getEntity();
		if (entity == null)
			return resp;
		Header cache = req.getFirstHeader("Cache-Control");
		if (cache != null && cache.getValue().contains("no-transform"))
			return resp;
		Header encoding = resp.getFirstHeader("Content-Encoding");
		if (encoding != null && "gzip".equals(encoding.getValue())) {
			resp.removeHeaders("Content-MD5");
			resp.removeHeaders("Content-Length");
			resp.setHeader("Content-Encoding", "identity");
			resp.addHeader("Warning", WARN_214);
			if (entity instanceof GZipEntity) {
				resp.setEntity(((GZipEntity) entity).getEntityDelegate());
			} else {
				resp.setEntity(new GUnzipEntity(entity));
			}
		}
		return resp;
	}

	private long getLength(Header hd, int length) {
		if (hd == null)
			return length;
		try {
			return Long.parseLong(hd.getValue());
		} catch (NumberFormatException e) {
			return length;
		}
	}

	private long getLength(HttpEntity entity, long length) {
		if (entity == null)
			return length;
		return entity.getContentLength();
	}

	protected boolean isCompressable(HttpMessage msg) {
		Header contentType = msg.getFirstHeader("Content-Type");
		if (contentType == null)
			return false;
		Header encoding = msg.getFirstHeader("Content-Encoding");
		Header cache = msg.getFirstHeader("Cache-Control");
		boolean identity = encoding == null || "identity".equals(encoding.getValue());
		boolean transformable = cache == null
				|| !cache.getValue().contains("no-transform");
		String type = contentType.getValue();
		boolean compressable = type.startsWith("text/")
				|| type.startsWith("application/xml")
				|| type.startsWith("application/x-turtle")
				|| type.startsWith("application/sparql-quey")
				|| type.startsWith("application/trix")
				|| type.startsWith("application/x-trig")
				|| type.startsWith("application/postscript")
				|| type.startsWith("application/javascript")
				|| type.startsWith("application/json")
				|| type.startsWith("application/mbox")
				|| type.startsWith("application/")
				&& (type.endsWith("+xml") || type.contains("+xml;"));
		return identity && compressable && transformable;
	}
}
