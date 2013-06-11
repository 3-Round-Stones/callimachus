/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.StreamingHttpEntity;
import org.callimachusproject.fluid.MediaType;
import org.callimachusproject.fluid.producers.HttpMessageReader;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.DelegatingFuture;
import org.callimachusproject.server.helpers.ResponseBuilder;

/**
 * Response body of {@link HttpResponse} and a configured content type
 * (message/x-response) are converted into the response itself.
 * 
 * @author James Leigh
 * 
 */
public class HttpResponseFilter implements AsyncExecChain {
	private static final List<String> CONTENT_HD = Arrays.asList(
			"Content-Type", "Content-Length", "Transfer-Encoding");
	private static final HttpMessageReader reader = new HttpMessageReader();

	private final AsyncExecChain delegate;
	private MediaType envelopeType;
	private String core;
	private String accept;

	public HttpResponseFilter(AsyncExecChain delegate) {
		this.delegate = delegate;
	}

	public String getEnvelopeType() {
		if (envelopeType == null)
			return null;
		return envelopeType.toString();
	}

	public void setEnvelopeType(String type) {
		if (type == null) {
			envelopeType = null;
			core = null;
			accept = null;
		} else {
			envelopeType = MediaType.valueOf(type);
			accept = envelopeType.toExternal() + ";q=0.1";
			core = type;
			if (core.contains(";")) {
				core = core.substring(0, core.indexOf(';'));
			}
		}
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target,
			final HttpRequest request, final HttpContext context,
			final FutureCallback<HttpResponse> callback) {
		if (envelopeType == null)
			return delegate.execute(target, request, context, callback);
		if (request.containsHeader("Accept")) {
			String hd = request.getFirstHeader("Accept").getValue();
			if (!hd.contains("*/*")) {
				request.addHeader("Accept", accept);
			}
		}
		final DelegatingFuture future = new DelegatingFuture(callback) {
			public void completed(HttpResponse result) {
				try {
					Header type = result.getFirstHeader("Content-Type");
					if (type != null && type.getValue().startsWith(core)
							&& envelopeType.match(type.getValue())) {
						result = new ResponseBuilder(request, context)
								.respond(unwrap(request, type.getValue(),
										result));
					}
					super.completed(result);
				} catch (IllegalArgumentException ex) {
					super.failed(ex);
				} catch (IOException ex) {
					super.failed(ex);
				}
			}
		};
		future.setDelegate(delegate.execute(target, request, context, future));
		return future;
	}

	HttpResponse unwrap(HttpRequest request, String type, HttpResponse resp)
			throws IOException {
		final HttpEntity entity = resp.getEntity();
		if (entity == null)
			return resp;
		InputStream in = entity.getContent();
		final ReadableByteChannel cin = ChannelUtil.newChannel(in);
		ReadableByteChannel ch = new ReadableByteChannel() {
			public boolean isOpen() {
				return cin.isOpen();
			}
			public void close() throws IOException {
				EntityUtils.consume(entity);
			}
			public int read(ByteBuffer dst) throws IOException {
				return cin.read(dst);
			}
			public String toString() {
				return cin.toString();
			}
		};
		HttpResponse response = (HttpResponse) reader.readFrom(type, ch);
		for (Header hd : resp.getAllHeaders()) {
			String name = hd.getName();
			if (!CONTENT_HD.contains(name) && !response.containsHeader(name)) {
				response.addHeader(hd);
			}
		}
		HttpEntity body = response.getEntity();
		if (body == null && !response.containsHeader("Content-Length")
				&& !response.containsHeader("Transfer-Encoding")) {
			response.setHeader("Content-Length", "0");
		} else if (response.containsHeader("Transfer-Encoding")
				&& response.getFirstHeader("Transfer-Encoding").getValue()
						.equals("identity")) {
			response.setHeader("Transfer-Encoding", "chunked");
			if (!body.isChunked()) {
				response.setEntity(new StreamingHttpEntity(body) {
					@Override
					public boolean isChunked() {
						return true;
					}
				});
			}
		}
		return response;
	}

}
