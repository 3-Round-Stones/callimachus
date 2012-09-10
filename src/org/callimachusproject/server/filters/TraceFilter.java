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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpDateGenerator;
import org.callimachusproject.server.model.EntityRemovedHttpResponse;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;

/**
 * Handles the TRACE and OPTIONS * requests.
 */
public class TraceFilter extends Filter {
    private static final HttpDateGenerator DATE_GENERATOR = new HttpDateGenerator();

	public TraceFilter(Filter delegate) {
		super(delegate);
	}

	public HttpResponse intercept(Request req) throws IOException {
		RequestLine line = req.getRequestLine();
		if ("TRACE".equals(req.getMethod())) {
			String CRLF = "\r\n";
			StringBuilder sb = new StringBuilder();
			sb.append("TRACE ").append(line.getUri()).append(" ");
			sb.append(line.getProtocolVersion());

			for (Header hd : req.getAllHeaders()) {
				sb.append(CRLF).append(hd.getName());
				sb.append(": ").append(hd.getValue());
			}

			sb.append(CRLF);
			ProtocolVersion ver = HttpVersion.HTTP_1_1;
			BasicHttpResponse resp = new EntityRemovedHttpResponse(ver, 200, "OK");
			resp.setHeader("Date", DATE_GENERATOR.getCurrentDate());
			NStringEntity entity = new NStringEntity(sb.toString(), "ISO-8859-1");
			entity.setContentType("message/http");
			entity.setChunked(false);
			resp.setEntity(entity);
			resp.setHeader("Content-Length", Long.toString(entity.getContentLength()));
			resp.setHeader(entity.getContentType());
			return resp;
		} else if ("OPTIONS".equals(req.getMethod())
				&& "*".equals(line.getUri())) {
			ProtocolVersion ver = HttpVersion.HTTP_1_1;
			BasicHttpResponse resp = new BasicHttpResponse(ver, 204, "No Content");
			resp.setHeader("Date", DATE_GENERATOR.getCurrentDate());
			resp.setHeader("Allow", "OPTIONS, TRACE, GET, HEAD, PUT, DELETE");
			return resp;
		} else {
			return allow(super.intercept(req));
		}
	}

	@Override
	public HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		return allow(super.filter(request, response));
	}

	private HttpResponse allow(HttpResponse resp) {
		if (resp != null && resp.getStatusLine().getStatusCode() == 405) {
			if (resp.containsHeader("Allow")) {
				String allow = resp.getFirstHeader("Allow").getValue();
				resp.setHeader("Allow", allow + ",TRACE");
			} else {
				resp.setHeader("Allow", "TRACE");
			}
		}
		return resp;
	}

}
