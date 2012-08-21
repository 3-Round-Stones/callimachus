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
package org.callimachusproject.server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.server.client.HTTPService;
import org.callimachusproject.server.model.ConsumingHttpEntity;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.tasks.Task;
import org.callimachusproject.server.tasks.TaskFactory;
import org.callimachusproject.server.util.ReadableContentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the filters and handles the HTTP requests.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectRequestHandler implements NHttpRequestHandler,
		HttpExpectationVerifier, EventListener, HTTPService {
	private static final String SELF = HTTPObjectRequestHandler.class.getName();
	public static final String HANDLER_ATTR = Task.class.getName();
	public static final String CONSUMING_ATTR = SELF + "#consuming";
	public static final String PENDING_ATTR = SELF + "#tasks";
	private static class ResponseTrigger implements NHttpResponseTrigger {
		private HttpResponse response;
		private IOException io;
		private HttpException http;

		public void submitResponse(HttpResponse response) {
			this.response = response;
		}

		public void handleException(IOException ex) {
			this.io = ex;
		}

		public void handleException(HttpException ex) {
			this.http = ex;
		}
	}

	private Logger logger = LoggerFactory
			.getLogger(HTTPObjectRequestHandler.class);
	private TaskFactory factory;
	private Set<NHttpConnection> connections = new HashSet<NHttpConnection>();

	public HTTPObjectRequestHandler(Filter filter, Handler handler,
			CallimachusRepository repository) {
		factory = new TaskFactory(repository, filter, handler);
	}

	public String getErrorPipe() {
		return factory.getErrorPipe();
	}

	public void setErrorPipe(String url) {
		factory.setErrorPipe(url);
	}

	public NHttpConnection[] getConnections() {
		synchronized (connections) {
			return connections.toArray(new NHttpConnection[connections.size()]);
		}
	}

	public HttpResponse service(HttpRequest request) throws IOException {
		Request req = new Request(request, InetAddress.getLocalHost());
		Task task = factory.createForegroundTask(req);
		ResponseTrigger trigger = new ResponseTrigger();
		task.setTrigger(trigger);
		try {
			if (trigger.io != null) {
				throw trigger.io;
			}
			if (trigger.http != null) {
				throw new IOException(trigger.http);
			}
			return trigger.response;
		} catch (IOException io) {
			if (trigger.response != null) {
				try {
					EntityUtils.consume(trigger.response.getEntity());
				} catch (IOException e) {
					logger.debug(e.toString(), e);
				}
			}
			throw io;
		}
	}

	public void verify(HttpRequest request, HttpResponse response,
			HttpContext ctx) throws HttpException {
		try {
			logger.debug("verify {}", request.getRequestLine());
			ReadableContentListener in = null;
			if (request instanceof HttpEntityEnclosingRequest) {
				in = new ReadableContentListener();
			}
			InetAddress remoteAddress = getRemoteAddress(ctx);
			Task task = factory.createBackgroundTask(process(request, in, remoteAddress));
			ctx.setAttribute(HANDLER_ATTR, task);
			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest req = (HttpEntityEnclosingRequest) request;
				ConsumingNHttpEntity reader = new ConsumingNHttpEntityTemplate(
						req.getEntity(), in);
				ctx.setAttribute(CONSUMING_ATTR, reader);
			}
			task.awaitVerification(1, TimeUnit.SECONDS); // block TCP stream
			HttpResponse resp = task.getHttpResponse();
			if (resp != null) {
				response.setStatusLine(resp.getStatusLine());
				for (Header hd : resp.getAllHeaders()) {
					response.removeHeaders(hd.getName());
				}
				for (Header hd : resp.getAllHeaders()) {
					response.addHeader(hd);
				}
				response.setEntity(resp.getEntity());
			}
		} catch (Exception e) {
			throw new HttpException(e.toString(), e);
		}
	}

	public ConsumingNHttpEntity entityRequest(
			HttpEntityEnclosingRequest request, HttpContext ctx)
			throws HttpException {
		try {
			logger.debug("entity request {}", request.getRequestLine());
			ConsumingNHttpEntity reader = (ConsumingNHttpEntity) ctx
					.removeAttribute(CONSUMING_ATTR);
			if (reader == null) {
				ReadableContentListener in = new ReadableContentListener();
				InetAddress remoteAddress = getRemoteAddress(ctx);
				Request req = process(request, in, remoteAddress);
				Task task = factory.createBackgroundTask(req);
				ctx.setAttribute(HANDLER_ATTR, task);
				return new ConsumingHttpEntity(request.getEntity(), in);
			} else {
				return reader;
			}
		} catch (Exception e) {
			throw new HttpException(e.toString(), e);
		}
	}

	public void handle(HttpRequest request, HttpResponse response,
			NHttpResponseTrigger trigger, final HttpContext ctx)
			throws HttpException {
		try {
			logger.debug("handle {}", request.getRequestLine());
			Task task = (Task) ctx.removeAttribute(HANDLER_ATTR);
			if (task == null) {
				InetAddress remoteAddress = getRemoteAddress(ctx);
				Request req = process(request, null, remoteAddress);
				task = factory.createBackgroundTask(req);
				task.setTrigger(trigger);
			} else {
				task.setTrigger(trigger);
			}
			Queue queue = (Queue) ctx.getAttribute(PENDING_ATTR);
			if (queue == null) {
				ctx.setAttribute(PENDING_ATTR, queue = new LinkedList<Task>());
			}
			synchronized (queue) {
				queue.add(task);
			}
			final Queue collection = queue;
			final Task item = task;
			task.onDone(new Runnable() {
				public void run() {
					synchronized (collection) {
						collection.remove(item);
					}
				}
			});
		} catch (Exception e) {
			throw new HttpException(e.toString(), e);
		}
	}

	public void connectionClosed(NHttpConnection conn) {
		abort(conn);
		logger.debug("{} closed", conn);
	}

	public void connectionOpen(NHttpConnection conn) {
		synchronized (connections) {
			connections.add(conn);
		}
		logger.debug("{} openned", conn);
	}

	public void connectionTimeout(NHttpConnection conn) {
		abort(conn);
		logger.debug("{} timed out", conn);
	}

	public void fatalIOException(IOException ex, NHttpConnection conn) {
		abort(conn);
		logger.debug("{} io error", conn);
	}

	public void fatalProtocolException(HttpException ex, NHttpConnection conn) {
		abort(conn);
		logger.debug("{} protocol error", conn);
	}

	private Request process(HttpRequest request, ReadableByteChannel in,
			InetAddress addr) {
		Request req = new Request(request, addr);
		if (in == null) {
			req.setEntity(null);
		} else {
			String type = req.getHeader("Content-Type");
			String length = req.getHeader("Content-Length");
			long size = -1;
			if (length != null) {
				size = Long.parseLong(length);
			}
			req.setEntity(new ReadableHttpEntityChannel(type, size, in));
		}
		return req;
	}

	private InetAddress getRemoteAddress(HttpContext context) {
		HttpInetConnection con = (HttpInetConnection) context
				.getAttribute(ExecutionContext.HTTP_CONNECTION);
		return con.getRemoteAddress();
	}

	private void abort(NHttpConnection conn) {
		abort(conn.getContext());
		HttpRequest req = conn.getHttpRequest();
		if (req != null && req instanceof HttpEntityEnclosingRequest) {
			try {
				EntityUtils.consume(((HttpEntityEnclosingRequest)req).getEntity());
			} catch (IOException e) {
				logger.warn(e.toString(), e);
			}
		}
		HttpResponse resp = conn.getHttpResponse();
		if (resp != null) {
			try {
				EntityUtils.consume(resp.getEntity());
			} catch (IOException e) {
				logger.warn(e.toString(), e);
			}
		}
		synchronized (connections) {
			connections.remove(conn);
		}
	}

	private void abort(HttpContext context) {
		ConsumingNHttpEntity reader = (ConsumingNHttpEntity) context
				.removeAttribute(CONSUMING_ATTR);
		if (reader != null) {
			try {
				reader.finish();
			} catch (IOException e) {
				logger.debug(e.toString(), e);
			}
		}
		Task task = (Task) context.removeAttribute(HANDLER_ATTR);
		if (task != null) {
			ResponseTrigger trigger = new ResponseTrigger();
			task.setTrigger(trigger);
			if (trigger.response != null) {
				try {
					EntityUtils.consume(trigger.response.getEntity());
				} catch (IOException e) {
					logger.debug(e.toString(), e);
				}
			}
			if (trigger.io != null) {
				logger.debug(trigger.io.toString(), trigger.io);
			}

			if (trigger.http != null) {
				logger.debug(trigger.http.toString(), trigger.http);
			}
			task.abort();
		}
	}

}