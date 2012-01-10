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
package org.callimachusproject.server.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.exceptions.GatewayTimeout;
import org.callimachusproject.server.model.ConsumingHttpEntity;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.util.ManagedExecutors;
import org.callimachusproject.server.util.ReadableContentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpCore interface to client HTTP event notifications.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectExecutionHandler implements
		NHttpRequestExecutionHandler, SessionRequestCallback {
	private final class AntiDeadlockTask implements Runnable {
		private Map<InetSocketAddress, FutureRequest> peeks = new HashMap<InetSocketAddress, FutureRequest>();

		public String toString() {
			return "check for starving request";
		}

		public void run() {
			synchronized (HTTPObjectExecutionHandler.this) {
				boolean empty = true;
				for (Map.Entry<InetSocketAddress, Queue<FutureRequest>> e : queues
						.entrySet()) {
					FutureRequest peek = e.getValue().peek();
					if (peek == null) {
						continue;
					} else if (peeks.get(e.getKey()) == peek) {
						connect(e.getKey());
					} else {
						peeks.put(e.getKey(), peek);
					}
				}
				if (empty) {
					schedule.cancel(false);
					schedule = null;
				}
			}
		}
	}

	private static final String CONN_ATTR = HTTPConnection.class.getName();
	private static ScheduledExecutorService scheduler = ManagedExecutors
			.getTimeoutThreadPool();
	private Logger logger = LoggerFactory
			.getLogger(HTTPObjectExecutionHandler.class);
	private Map<InetSocketAddress, Queue<FutureRequest>> queues = new HashMap<InetSocketAddress, Queue<FutureRequest>>();
	private Map<InetSocketAddress, List<HTTPConnection>> connections = new HashMap<InetSocketAddress, List<HTTPConnection>>();
	private final Filter filter;
	private final ConnectingIOReactor connector;
	private final ConnectingIOReactor sslconnector;
	private ScheduledFuture<?> schedule;
	private String agent;
	private String via;
	private InetAddress localhost;

	public HTTPObjectExecutionHandler(Filter filter,
			ConnectingIOReactor connector,
			ConnectingIOReactor sslconnector) throws UnknownHostException {
		this.filter = filter;
		this.connector = connector;
		this.sslconnector = sslconnector;
		localhost = InetAddress.getLocalHost();
	}

	public synchronized HTTPConnection[] getConnections() {
		List<HTTPConnection> list = new ArrayList<HTTPConnection>();
		for (List<HTTPConnection> v : connections.values()) {
			list.addAll(v);
		}
		HTTPConnection[] ret = new HTTPConnection[list.size()];
		return list.toArray(ret);
	}

	public String getAgentName() {
		return agent;
	}

	public void setAgentName(String agent) {
		this.agent = agent;
		via = "1.1 " + getHostName() + "  (" + agent + ")";
	}

	public HttpResponse service(InetSocketAddress remoteAddress,
			HttpRequest request) throws IOException, GatewayTimeout {
		try {
			return submitRequest(remoteAddress, request).get();
		} catch (ExecutionException e) {
			try {
				throw e.getCause();
			} catch (RuntimeException cause) {
				if (cause.getCause() == null) {
					cause.initCause(new RuntimeException(e.toString()));
				}
				throw cause;
			} catch (Error cause) {
				throw cause;
			} catch (Throwable cause) {
				throw new IOException(e);
			}
		} catch (InterruptedException e) {
			throw new GatewayTimeout(e);
		}
	}

	public synchronized void completed(SessionRequest request) {
		HTTPConnection conn = (HTTPConnection) request.getAttachment();
		conn.setIOSession(request.getSession());
		removeIdleConnection(conn);
	}

	public synchronized void cancelled(SessionRequest request) {
		HTTPConnection conn = (HTTPConnection) request.getAttachment();
		debug("cancelled", conn);
		InetSocketAddress addr = discard(conn);
		Queue<FutureRequest> queue = queues.get(addr);
		if (queue != null) {
			FutureRequest freq;
			String msg = "Connection aborted: " + addr.toString();
			GatewayTimeout gt = new GatewayTimeout(msg);
			while ((freq = queue.poll()) != null) {
				freq.set(gt);
			}
		}
	}

	public synchronized void failed(SessionRequest request) {
		HTTPConnection conn = (HTTPConnection) request.getAttachment();
		conn.setIOException(request.getException());
		debug("failed", conn);
		InetSocketAddress addr = discard(conn);
		Queue<FutureRequest> queue = queues.get(addr);
		if (queue != null) {
			FutureRequest freq;
			String msg = "Could Not Connect to " + addr.toString();
			GatewayTimeout gt = new GatewayTimeout(msg);
			while ((freq = queue.poll()) != null) {
				freq.set(gt);
			}
		}
	}

	public synchronized void timeout(SessionRequest request) {
		HTTPConnection conn = (HTTPConnection) request.getAttachment();
		debug("timeout", conn);
		InetSocketAddress addr = discard(conn);
		Queue<FutureRequest> queue = queues.get(addr);
		if (queue != null) {
			FutureRequest freq;
			String msg = "Connection Timed out: " + addr.toString();
			GatewayTimeout gt = new GatewayTimeout(msg);
			while ((freq = queue.poll()) != null) {
				freq.set(gt);
			}
		}
	}

	public void initalizeContext(HttpContext context, Object conn) {
		assert conn != null;
		context.setAttribute(CONN_ATTR, conn);
		((HTTPConnection)conn).requestOutput();
	}

	public synchronized void finalizeContext(HttpContext context) {
		HTTPConnection conn = getHTTPConnection(context);
		InetSocketAddress addr = discard(conn);
		FutureRequest freq;
		if (conn.getResponseCount() < 1) {
			String msg = "Connection was reset: " + addr.toString();
			GatewayTimeout gt = new GatewayTimeout(msg);
			while ((freq = conn.removeRequest()) != null) {
				freq.set(gt);
			}
		} else {
			while ((freq = conn.removeRequest()) != null) {
				submitRequest(addr, freq);
			}
		}
	}

	public synchronized HttpRequest submitRequest(HttpContext context) {
		final HTTPConnection conn = getHTTPConnection(context);
		if (conn.getReading() != null) {
			debug("blocked", conn);
			return null; // don't submit request if reading previous response
		}
		SocketAddress addr = conn.getRemoteAddress();
		Queue<FutureRequest> queue = queues.get(addr);
		if (queue == null)
			return null;
		try {
			FutureRequest freq = queue.poll();
			if (freq == null)
				return null;
			conn.addRequest(freq);
			HttpRequest req = freq.getHttpRequest();
			req.setHeader("Connection", "keep-alive");
			if (req.containsHeader("User-Agent")) {
				req.addHeader("Via", via);
			} else {
				req.setHeader("User-Agent", agent);
			}
			debug("sent", req);
			Request request = new Request(req, localhost);
			HttpResponse interception = filter.intercept(request);
			if (interception != null) {
				freq.setRequest(request);
				freq.set(interception);
				return submitRequest(context);
			}
			Request filtered = filter.filter(request);
			freq.setRequest(filtered);
			if (filtered.getEntity() == null) {
				req = new BasicHttpRequest(filtered.getRequestLine());
				req.setHeaders(filtered.getAllHeaders());
			} else {
				if (!filtered.containsHeader("Transfer-Encoding")
						&& !filtered.containsHeader("Content-Length")
						&& filtered.getEntity().getContentLength() < 0) {
					filtered.addHeader("Transfer-Encoding", "chunked");
				}
				req = filtered;
			}
			return req;
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return null;
		} finally {
			if (queue.isEmpty()) {
				queues.remove(addr);
			}
		}
	}

	public ConsumingNHttpEntity responseEntity(HttpResponse response,
			HttpContext context) throws IOException {
		HTTPConnection conn = getHTTPConnection(context);
		FutureRequest req = conn.removeRequest();
		conn.setReading(req);
		ConsumingNHttpEntity consume = filter.consume(req.getRequest(),
				response);
		if (consume == null) {
			ReadableContentListener in = new ReadableContentListener();
			String type = getHeader(response, "Content-Type");
			String length = getHeader(response, "Content-Length");
			long size = -1;
			if (length != null) {
				size = Long.parseLong(length);
			}
			response.setEntity(new ReadableHttpEntityChannel(type, size, in));
			req.set(response);
			debug("reading", req.getRequest());
			return new ConsumingHttpEntity(response.getEntity(), in);
		} else {
			debug("caching", req.getRequest());
			return consume;
		}
	}

	public void handleResponse(HttpResponse response, HttpContext context)
			throws IOException {
		final HTTPConnection conn = getHTTPConnection(context);
		FutureRequest req = conn.getReading();
		if (req == null) {
			req = conn.removeRequest();
			req.set(filter.filter(req.getRequest(), response));
			debug("responded", req.getRequest());
		} else {
			if (req.poll() == null) {
				req.set(filter.filter(req.getRequest(), response));
			}
			debug("completed", req.getRequest());
			conn.setReading(null); // input will no longer block new requests
			conn.requestOutput();
		}
		removeIdleConnection(conn);
	}

	private synchronized Future<HttpResponse> submitRequest(
			final InetSocketAddress remoteAddress, HttpRequest request)
			throws IOException {
		FutureRequest result = new FutureRequest(request) {
			protected boolean cancel() {
				super.cancel();
				return remove(remoteAddress, this);
			}
		};
		HttpResponse interception = filter.intercept(new Request(request, localhost));
		if (interception == null) {
			submitRequest(remoteAddress, result);
		} else {
			logger.debug("{} was {}", request.getRequestLine(), interception
					.getStatusLine());
			result.set(interception);
		}
		return result;
	}

	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "locahost";
		}
	}

	private void submitRequest(final InetSocketAddress remoteAddress,
			FutureRequest request) {
		Queue<FutureRequest> queue = queues.get(remoteAddress);
		if (queue == null) {
			queues.put(remoteAddress, queue = new LinkedList<FutureRequest>());
		}
		queue.add(request);
		Collection<HTTPConnection> sessions = connections.get(remoteAddress);
		if (sessions != null && !sessions.isEmpty()) {
			for (HTTPConnection session : sessions) {
				if (session.getIOSession() == null
						|| session.getReading() == null) {
					session.requestOutput();
					if (schedule == null) {
						schedule = scheduler.scheduleWithFixedDelay(
								new AntiDeadlockTask(), 0, 5, TimeUnit.SECONDS);
					}
					return;
				}
			}
		}
		connect(remoteAddress);
	}

	private synchronized InetSocketAddress discard(HTTPConnection conn) {
		InetSocketAddress remoteAddress = conn.getRemoteAddress();
		List<HTTPConnection> list = connections.get(remoteAddress);
		if (list != null && list.isEmpty()) {
			connections.remove(remoteAddress);
		} else if (list != null) {
			list.remove(conn);
		}
		return remoteAddress;
	}

	private void removeIdleConnection(final HTTPConnection conn) {
		final int count = conn.getRequestCount();
		if (!conn.isPendingRequest()) {
			scheduler.schedule(new Runnable() {
				public String toString() {
					return "remove idle connections";
				}

				public void run() {
					synchronized (HTTPObjectExecutionHandler.this) {
						SocketAddress addr = conn.getRemoteAddress();
						Queue<FutureRequest> queue = queues.get(addr);
						if (queue == null || queue.isEmpty()) {
							removeIdleConnection(conn, count);
						} else {
							conn.requestOutput();
							if (conn.getRequestCount() == count) {
								scheduler.schedule(this, 5, TimeUnit.SECONDS);
							}
						}
					}
				}
			}, 5, TimeUnit.SECONDS);
		}
	}

	private synchronized void removeIdleConnection(HTTPConnection conn,
			int count) {
		if (conn.getRequestCount() == count) {
			SocketAddress remoteAddress = conn.getRemoteAddress();
			List<HTTPConnection> list = connections.get(remoteAddress);
			if (list != null && list.isEmpty()) {
				connections.remove(remoteAddress);
			} else if (list != null) {
				list.remove(conn);
			}
			try {
				debug("closed", conn);
				conn.shutdown();
			} catch (IOException e) {
				logger.warn(e.toString(), e);
			}
		}
	}

	private String getHeader(HttpResponse response, String name) {
		Header hd = response.getFirstHeader(name);
		if (hd == null)
			return null;
		return hd.getValue();
	}

	private synchronized void connect(InetSocketAddress remoteAddress) {
		HTTPConnection conn = new HTTPConnection(remoteAddress);
		if (remoteAddress instanceof SecureSocketAddress) {
			sslconnector.connect(remoteAddress, null, conn, this);
		} else {
			connector.connect(remoteAddress, null, conn, this);
		}
		List<HTTPConnection> sessions = connections.get(remoteAddress);
		if (sessions == null) {
			connections.put(remoteAddress,
					sessions = new LinkedList<HTTPConnection>());
		}
		sessions.add(conn);
	}

	private synchronized boolean remove(SocketAddress addr,
			FutureRequest request) {
		Queue<FutureRequest> queue = queues.get(addr);
		if (queue == null)
			return false;
		try {
			return queue.remove(request);
		} finally {
			if (queue.isEmpty()) {
				queues.remove(addr);
			}
		}
	}

	private HTTPConnection getHTTPConnection(HttpContext context) {
		return (HTTPConnection) context.getAttribute(CONN_ATTR);
	}

	private void debug(String msg, HttpRequest req) {
		if (logger.isDebugEnabled()) {
			logger.debug("{} {} {}", new Object[] { Thread.currentThread(),
					msg, req.getRequestLine() });
		}
	}

	private void debug(String msg, HTTPConnection conn) {
		if (logger.isDebugEnabled()) {
			logger.debug("{} {} {}", new Object[] { Thread.currentThread(),
					msg, conn });
		}
	}
}
