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
package org.callimachusproject.server.filters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.HttpEntityWrapper;
import org.callimachusproject.fluid.MediaType;
import org.callimachusproject.fluid.producers.HttpMessageReader;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.util.ChannelUtil;

/**
 * Response body of {@link HttpResponse} and a configured content type
 * (message/x-response) are converted into the response itself.
 * 
 * @author James Leigh
 * 
 */
public class HttpResponseFilter extends Filter {
	private static final List<String> CONTENT_HD = Arrays.asList(
			"Content-Type", "Content-Length", "Transfer-Encoding");
	private static final HttpMessageReader reader = new HttpMessageReader();
	private MediaType envelopeType;
	private String core;
	private String accept;

	public HttpResponseFilter(Filter delegate) {
		super(delegate);
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
	public Request filter(Request request) throws IOException {
		if (envelopeType != null && request.containsHeader("Accept")) {
			String hd = request.getFirstHeader("Accept").getValue();
			if (!hd.contains("*/*")) {
				request.addHeader("Accept", accept);
			}
		}
		return super.filter(request);
	}

	@Override
	public HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		response = super.filter(request, response);
		if (envelopeType == null)
			return response;
		Header type = response.getFirstHeader("Content-Type");
		if (type != null && type.getValue().startsWith(core)) {
			try {
				if (envelopeType.match(type.getValue())) {
					return unwrap(request, type.getValue(), response);
				}
			} catch (IllegalArgumentException e) {
				return response;
			} catch (IOException e) {
				return response;
			}
		}
		return response;
	}

	private HttpResponse unwrap(Request request, String type, HttpResponse resp)
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
				response.setEntity(new HttpEntityWrapper(body) {
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
