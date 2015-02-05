/*
 * Copyright (c) 2014 3 Round Stones Inc., Some rights reserved.
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.execchain.ClientExecChain;
import org.callimachusproject.io.LatencyInputStream;

/**
 * Reads the head/prolog of the response body before the
 * {@link #execute(HttpRoute, HttpRequestWrapper, HttpClientContext, HttpExecutionAware)}
 * methods finishes. If it reads the entire response, it also sets the
 * contentLength property.
 *
 * @author James Leigh
 *
 */
public class ContentPeekHandler implements ClientExecChain {
	private static final int MIN_RESPONSE_SIZE = 65536;
	private final int size;
	private final ClientExecChain delegate;

	public ContentPeekHandler(ClientExecChain delegate) {
		this(delegate, MIN_RESPONSE_SIZE);
	}

	public ContentPeekHandler(ClientExecChain delegate, int size) {
		this.delegate = delegate;
		this.size = size;
	}

	public CloseableHttpResponse execute(HttpRoute route,
			HttpRequestWrapper request, HttpClientContext clientContext,
			HttpExecutionAware execAware) throws IOException, HttpException {
		CloseableHttpResponse resp = delegate.execute(route, request,
				clientContext, execAware);
		HttpEntity entity = resp.getEntity();
		if (entity == null)
			return resp;
		LatencyInputStream in = new LatencyInputStream(entity.getContent(), size);
		in.mark(size);
		int count = 0;
		long read;
		do {
			read = in.skip(size - count);
			count += read;
		} while (count < size && read > 0);
		in.reset();
		BasicHttpEntity e = new BasicHttpEntity();
		e.setContentType(entity.getContentType());
		e.setContentEncoding(entity.getContentEncoding());
		e.setContent(in);
		if (read <= 0) {
			e.setChunked(false);
			e.setContentLength(count);
		} else {
			e.setChunked(entity.isChunked());
			e.setContentLength(entity.getContentLength());
		}
		resp.setEntity(e);
		return resp;
	}

}
