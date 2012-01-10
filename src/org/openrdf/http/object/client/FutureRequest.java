/*
 * Copyright (c) 2010, Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.openrdf.http.object.exceptions.GatewayTimeout;
import org.openrdf.http.object.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle used to track received responses with their requests.
 * 
 * @author James Leigh
 * 
 */
public class FutureRequest implements Future<HttpResponse> {
	private Logger logger = LoggerFactory.getLogger(FutureRequest.class);
	private boolean cancelled;
	private Exception ex;
	private HttpRequest req;
	private Request request;
	private HttpResponse result;
	private HTTPConnection conn;
	private Throwable source;

	public FutureRequest(HttpRequest req) {
		this.req = req;
		if (logger.isDebugEnabled()) {
			source = new IllegalStateException(req.getRequestLine().toString());
		}
	}

	public synchronized void attached(HTTPConnection conn) {
		this.conn = conn;
		notifyAll();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(req.getRequestLine());
		if (result != null) {
			sb.append(" ").append(result.getStatusLine());
			if (result.getEntity() != null) {
				sb.append(" ").append(result.getEntity());
			}
		}
		if (ex != null) {
			sb.append(" ").append(ex);
		}
		if (cancelled) {
			sb.append(" cancelled");
		}
		return sb.toString();
	}

	public HttpRequest getHttpRequest() {
		return req;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request req) {
		this.request = req;
	}

	public synchronized void set(HttpResponse result) throws IOException {
		closeRequest();
		assert result != null;
		this.result = result;
		HttpEntity entity = result.getEntity();
		if ("HEAD".equals(req.getRequestLine().getMethod()) && entity != null) {
			entity.consumeContent();
			result.setEntity(null);
		} else if (entity != null && source != null) {
			result.setEntity(new TrackedHttpEntity(entity, source));
		}
		notifyAll();
	}

	public synchronized void set(GatewayTimeout ex) {
		closeRequest();
		assert ex != null;
		this.ex = ex;
		notifyAll();
	}

	public synchronized void set(RuntimeException ex) {
		closeRequest();
		assert ex != null;
		this.ex = ex;
		notifyAll();
	}

	public synchronized void set(IOException ex) {
		closeRequest();
		assert ex != null;
		this.ex = ex;
		notifyAll();
	}

	public synchronized HttpResponse get() throws InterruptedException,
			ExecutionException {
		while (!isDone()) {
			if (conn != null) {
				conn.requestInput();
			}
			debug("waiting on");
			wait();
			if (isDone()) {
				debug("received");
			}
		}
		if (ex != null)
			throw new ExecutionException(ex);
		return poll();
	}

	public synchronized HttpResponse poll() {
		return result;
	}

	public synchronized HttpResponse get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		if (!isDone()) {
			if (conn != null) {
				conn.requestInput();
			}
			debug("waiting on");
			wait(unit.toMillis(timeout));
			if (!isDone())
				throw new TimeoutException("Timeout while waiting for "
						+ req.getRequestLine().toString());
			debug("received");
		}
		if (ex != null)
			throw new ExecutionException(ex);
		return poll();
	}

	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		debug("canceled");
		cancelled = cancel();
		notifyAll();
		return cancelled;
	}

	public synchronized boolean isCancelled() {
		return cancelled;
	}

	public synchronized boolean isDone() {
		return ex != null || result != null || isCancelled();
	}

	protected boolean cancel() {
		closeRequest();
		return false;
	}

	private void closeRequest() {
		if (request != null) {
			request.close();
		}
	}

	private void debug(String msg) {
		if (logger.isDebugEnabled()) {
			logger.debug("{} {} {}", new Object[] { Thread.currentThread(),
					msg, req.getRequestLine() });
		}
	}

}
