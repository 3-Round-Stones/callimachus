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
package org.callimachusproject.server.model;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.AbstractHttpMessage;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.Conflict;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.server.exceptions.NotFound;
import org.callimachusproject.server.exceptions.ResponseException;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.exceptions.BehaviourException;
import org.openrdf.sail.optimistic.exceptions.ConcurrencyException;

/**
 * Builds an HTTP response.
 * 
 * @author James Leigh
 */
public class Response extends AbstractHttpMessage {
	private final ResponseParameter entity;
	private ResponseException exception;
	private long lastModified;
	private Class<?> type;
	private int status = 204;
	private String phrase = "No Content";
	private List<Runnable> onclose = new LinkedList<Runnable>();

	public Response() {
		this.entity = null;
	}

	public Response(ResponseParameter entity) {
		this.entity = entity;
		status(200, "OK");
	}

	public Response(HttpResponse message, ObjectConnection con)
			throws IOException {
		StatusLine status = message.getStatusLine();
		status(status.getStatusCode(), status.getReasonPhrase());
		for (Header hd : message.getAllHeaders()) {
			removeHeaders(hd.getName());
		}
		for (Header hd : message.getAllHeaders()) {
			addHeader(hd);
		}
		HttpEntity entity = message.getEntity();
		if (entity == null) {
			this.entity = null;
		} else {
			String[] mimeTypes = new String[0];
			Header encoding = entity.getContentEncoding();
			if (encoding != null) {
				setHeader(encoding);
			}
			Header type = entity.getContentType();
			if (type != null) {
				setHeader(type);
				mimeTypes = new String[] { type.getValue() };
			}
			long length = entity.getContentLength();
			if (length >= 0) {
				setHeader("Content-Length", Long.toString(length));
			}
			InputStream in = entity.getContent();
			this.type = InputStream.class;
			this.entity = new ResponseParameter(mimeTypes, in, this.type,
					this.type, null, con);
		}
	}

	public Response onClose(Runnable task) {
		onclose.add(task);
		return this;
	}

	public List<Runnable> getOnClose() {
		return onclose;
	}

	public Response exception(ResponseException e) {
		status(e.getStatusCode(), e.getShortMessage());
		this.exception = e;
		assert this.entity == null;
		return this;
	}

	public Response badRequest(Exception e) {
		return exception(new BadRequest(e));
	}

	public Response conflict(ConcurrencyException e) {
		return exception(new Conflict(e));
	}

	public Class<?> getEntityType() {
		return type;
	}

	public ResponseException getException() {
		return exception;
	}

	public String getHeader(String header) {
		Header hd = getFirstHeader(header);
		if (hd == null)
			return null;
		return hd.getValue();
	}

	public Long getLastModified() {
		return lastModified;
	}

	public long getSize(String mimeType) {
		return entity.getSize(mimeType);
	}

	public int getStatus() {
		return getStatusCode();
	}

	public String getMessage() {
		return phrase;
	}

	public Response header(String header, String value) {
		if (value == null) {
			removeHeaders(header);
		} else {
			String existing = getHeader(header);
			if (existing == null) {
				setHeader(header, value);
			} else if (!existing.equals(value)) {
				setHeader(header, existing + "," + value);
			}
		}
		return this;
	}

	public boolean isContent() {
		return entity != null || exception != null;
	}

	public boolean isException() {
		return exception != null;
	}

	public boolean isNoContent() {
		return getStatusCode() == 204;
	}

	public boolean isOk() {
		return getStatusCode() == 200;
	}

	public long lastModified() {
		return lastModified;
	}

	public Response lastModified(long lastModified, String text) {
		if (lastModified <= 0)
			return this;
		lastModified = lastModified / 1000 * 1000;
		long pre = this.lastModified;
		if (pre >= lastModified)
			return this;
		this.lastModified = lastModified;
		setHeader("Last-Modified", text);
		return this;
	}

	public Response location(String location) {
		header("Location", location);
		return this;
	}

	public Response noContent() {
		status(204, "No Content");
		assert this.entity == null;
		return this;
	}

	public Response notFound() {
		return exception(new NotFound());
	}

	public Response notModified() {
		status(304, "Not Modified");
		assert this.entity == null;
		return this;
	}

	public Response preconditionFailed() {
		status(412, "Precondition Failed");
		assert this.entity == null;
		return this;
	}

	public Response preconditionFailed(String notice) {
		status(412, notice);
		assert this.entity == null;
		return this;
	}

	public Response server(Exception error) {
		if (isWrapper(error) && error.getCause() instanceof Exception) {
			server((Exception) error.getCause());
		}
		return exception(new InternalServerError(error));
	}

	public void setEntityType(Class<?> type) {
		this.type = type;
	}

	public Response status(int status, String msg) {
		if (msg.indexOf('\n') < 0 && msg.indexOf('\r') < 0) {
			this.phrase = msg;
		} else {
			this.phrase = msg.replaceAll("\\s+", " ").trim();
		}
		this.status = status;
		return this;
	}

	public String toString() {
		return phrase;
	}

	public ReadableByteChannel write(String mimeType)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		return entity.write(mimeType).asChannel();
	}

	public int getStatusCode() {
		return status;
	}

	public ProtocolVersion getProtocolVersion() {
		return new ProtocolVersion("HTTP", 1, 1);
	}

	private boolean isWrapper(Exception ex) {
		return ex instanceof BehaviourException
				|| ex instanceof InvocationTargetException
				|| ex instanceof ExecutionException;
	}

}
