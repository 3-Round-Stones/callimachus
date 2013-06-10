/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.chain;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.callimachusproject.client.CloseableEntity;
import org.callimachusproject.client.GUnzipEntity;
import org.callimachusproject.client.GZipEntity;

/**
 * Compresses safe responses.
 */
public class GZipFilter implements ClientExecChain {
	private final ClientExecChain delegate;

	public GZipFilter(ClientExecChain delegate) {
		this.delegate = delegate;
	}

	@Override
	public CloseableHttpResponse execute(HttpRoute route,
			HttpRequestWrapper request, HttpClientContext context,
			HttpExecutionAware execAware) throws IOException, HttpException {
		CloseableHttpResponse resp = delegate.execute(route, request, context, execAware);
		String method = request.getRequestLine().getMethod();
		int code = resp.getStatusLine().getStatusCode();
		boolean safe = method.equals("HEAD") || method.equals("GET") || method.equals("PROFIND");
		boolean compressed = isAlreadyCompressed(resp.getEntity());
		if (code < 500 && safe && isCompressable(resp) || compressed) {
			long len = getContentLength(resp);
			if (compressed || len < 0 || len > 500) {
				resp.removeHeaders("Content-MD5");
				resp.removeHeaders("Content-Length");
				resp.setHeader("Transfer-Encoding", "chunked");
				resp.setHeader("Content-Encoding", "gzip");
				resp.setEntity(gzip(resp.getEntity()));
			}
		}
		return resp;
	}

	private boolean isAlreadyCompressed(HttpEntity entity) {
		if (entity instanceof GUnzipEntity)
			return true;
		if (entity instanceof CloseableEntity)
			return isAlreadyCompressed(((CloseableEntity) entity).getEntityDelegate());
		return false;
	}

	private long getContentLength(HttpResponse resp) {
		if (resp.getEntity() != null)
			return resp.getEntity().getContentLength();
		Header length = resp.getFirstHeader("Content-Length");
		if (length == null)
			return -1;
		return Long.parseLong(length.getValue());
	}

	private HttpEntity gzip(HttpEntity entity) {
		if (entity instanceof GUnzipEntity)
			return ((GUnzipEntity) entity).getEntityDelegate();
		if (entity instanceof CloseableEntity) {
			CloseableEntity centity = (CloseableEntity) entity;
			centity.setEntityDelegate(gzip(centity.getEntityDelegate()));
			return centity;
		}
		return new GZipEntity(entity);
	}

	private boolean isCompressable(HttpResponse msg) {
		if (msg.getEntity() == null)
			return false;
		Header contentType = msg.getFirstHeader("Content-Type");
		if (contentType == null) {
			contentType = msg.getEntity().getContentType();
		}
		if (contentType == null)
			return false;
		for (Header hd : msg.getHeaders("Cache-Control")) {
			if (hd.getValue().contains("no-transform"))
				return false;
		}
		Header encoding = msg.getFirstHeader("Content-Encoding");
		boolean identity = encoding == null || "identity".equals(encoding.getValue());
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
		return identity && compressable;
	}
}
