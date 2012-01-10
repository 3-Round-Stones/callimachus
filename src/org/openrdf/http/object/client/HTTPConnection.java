/*
 * Copyright (c) 2010, Zepheira LLC Some rights reserved.
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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 * Tracks the pending requests sent on a connection and which request is being
 * received.
 * 
 * @author James Leigh
 * 
 */
public class HTTPConnection {
	private int requests;
	private int responses;
	private InetSocketAddress remoteAddress;
	private IOException io;
	private IOSession session;
	private HttpContext context;
	private Queue<FutureRequest> queue = new LinkedList<FutureRequest>();
	private FutureRequest reading;
	private NHttpConnection conn;

	public HTTPConnection(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	@Override
	public String toString() {
		if (session == null)
			return remoteAddress.toString();
		return remoteAddress.toString() + "<-" + session.getLocalAddress()
				+ session.toString();
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public SocketAddress getLocalAddress() {
		if (session == null)
			return null;
		return session.getLocalAddress();
	}

	public int getStatus() {
		if (conn == null)
			return NHttpConnection.CLOSED;
		return conn.getStatus();
	}

	public boolean isOpen() {
		if (conn == null)
			return false;
		return conn.isOpen();
	}

	public boolean isStale() {
		if (conn == null)
			return true;
		return conn.isStale();
	}

	public IOException getIOException() {
		return io;
	}

	public void setIOException(IOException io) {
		this.io = io;
	}

	public void requestInput() {
		if (conn != null) {
			conn.requestInput();
		}
	}

	public void requestOutput() {
		if (conn != null) {
			conn.requestOutput();
		}
	}

	public void shutdown() throws IOException {
		if (conn != null) {
			conn.shutdown();
		}
	}

	public int getRequestCount() {
		return requests;
	}

	public int getResponseCount() {
		return responses;
	}

	public synchronized boolean isPendingRequest() {
		return !queue.isEmpty();
	}

	public synchronized FutureRequest[] getPendingRequests() {
		return queue.toArray(new FutureRequest[queue.size()]);
	}

	public FutureRequest getReading() {
		return reading;
	}

	public void setReading(FutureRequest req) {
		this.reading = req;
	}

	public IOSession getIOSession() {
		return session;
	}

	public synchronized void setIOSession(IOSession session) {
		this.session = session;
		conn = (NHttpConnection) session
				.getAttribute(ExecutionContext.HTTP_CONNECTION);
		for (FutureRequest req : queue) {
			req.attached(this);
		}
	}

	public HttpContext getHttpContext() {
		return context;
	}

	public void setHttpContext(HttpContext context) {
		this.context = context;
	}

	public synchronized void addRequest(FutureRequest req) {
		requests++;
		queue.add(req);
		req.attached(this);
	}

	public synchronized FutureRequest removeRequest() {
		responses++;
		return queue.poll();
	}

}
