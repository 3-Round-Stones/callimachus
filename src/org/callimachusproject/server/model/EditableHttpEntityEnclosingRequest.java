/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.model;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.HttpParams;

/**
 * Allows the request line and entity to be changed.
 * 
 * @author James Leigh
 *
 */
public class EditableHttpEntityEnclosingRequest implements
		HttpEntityEnclosingRequest, Cloneable {
	private HttpRequest request;
	private RequestLine requestLine;
	private HttpEntity entity;
	private HttpParams params;

	public EditableHttpEntityEnclosingRequest(HttpRequest request) {
		this.request = request;
		this.requestLine = request.getRequestLine();
		this.params = request.getParams();
	}

	public HttpRequest getEnclosingRequest() {
		return request;
	}

	@Override
	public EditableHttpEntityEnclosingRequest clone() {
		EditableHttpEntityEnclosingRequest cloned;
		try {
			cloned = (EditableHttpEntityEnclosingRequest) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
		cloned.request = new BasicHttpEntityEnclosingRequest(requestLine);
		cloned.request.setHeaders(request.getAllHeaders());
		cloned.request.setParams(params);
		cloned.setEntity(getEntity());
		return cloned;
	}

	public ProtocolVersion getProtocolVersion() {
		return getRequestLine().getProtocolVersion();
	}

	public RequestLine getRequestLine() {
		return requestLine;
	}

	public void setRequestLine(RequestLine requestLine) {
		this.requestLine = requestLine;
	}

	public boolean expectContinue() {
		if (request instanceof HttpEntityEnclosingRequest)
			return ((HttpEntityEnclosingRequest) request).expectContinue();
		return false;
	}

	public HttpEntity getEntity() {
		if (entity != null)
			return entity;
		if (request instanceof HttpEntityEnclosingRequest)
			return ((HttpEntityEnclosingRequest) request).getEntity();
		return null;
	}

	public void setEntity(HttpEntity entity) {
		if (entity == null && request instanceof HttpEntityEnclosingRequest) {
			((HttpEntityEnclosingRequest) request).setEntity(entity);
		} else {
			this.entity = entity;
		}
	}

	public void addHeader(Header header) {
		request.addHeader(header);
	}

	public void addHeader(String name, String value) {
		request.addHeader(name, value);
	}

	public boolean containsHeader(String name) {
		return request.containsHeader(name);
	}

	public Header[] getAllHeaders() {
		return request.getAllHeaders();
	}

	public Header getFirstHeader(String name) {
		return request.getFirstHeader(name);
	}

	public Header[] getHeaders(String name) {
		return request.getHeaders(name);
	}

	public Header getLastHeader(String name) {
		return request.getLastHeader(name);
	}

	public HttpParams getParams() {
		return params;
	}

	public HeaderIterator headerIterator() {
		return request.headerIterator();
	}

	public HeaderIterator headerIterator(String name) {
		return request.headerIterator(name);
	}

	public void removeHeader(Header header) {
		request.removeHeader(header);
	}

	public void removeHeaders(String name) {
		request.removeHeaders(name);
	}

	public void setHeader(Header header) {
		request.setHeader(header);
	}

	public void setHeader(String name, String value) {
		request.setHeader(name, value);
	}

	public void setHeaders(Header[] headers) {
		request.setHeaders(headers);
	}

	public void setParams(HttpParams params) {
		this.params = params;
	}

}
