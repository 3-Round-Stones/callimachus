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
package org.callimachusproject.server.process;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.AbstractHttpClient;
import org.callimachusproject.client.UnavailableHttpClient;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.callimachusproject.xproc.Pipeline;
import org.callimachusproject.xproc.PipelineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the HTTP requests.
 * 
 * @author James Leigh
 * 
 */
public class HTTPObjectRequestHandler extends AbstractHttpClient implements
		HttpAsyncRequestHandler<HttpRequest> {
	private static final String SELF = HTTPObjectRequestHandler.class.getName();
	private static final String EXCHANGE_ATTR = SELF + "#exchange";
	private static final String PROCESSING_ATTR = SELF + "#processing";
	private static final StatusLine _503 = new BasicStatusLine(HttpVersion.HTTP_1_1, 503, "Invalid Service State");
	private static final InetAddress LOCALHOST = DomainNameSystemResolver.getInstance().getLocalHost();

	private final Logger logger = LoggerFactory
			.getLogger(HTTPObjectRequestHandler.class);
	private final Map<String, CalliRepository> repositories = new LinkedHashMap<String, CalliRepository>();
	private final Filter filter;
	private final Handler handler;
	private Pipeline pipeline;
	private RequestTriagerActor requestTriager;
	private RequestTransactionActor requestHandler;

	public HTTPObjectRequestHandler(Filter filter, Handler handler) {
		this.filter = filter;
		this.handler = handler;
	}

	public synchronized void addOrigin(String origin, CalliRepository repository) {
		repositories.put(origin, repository);
		if (requestHandler != null) {
			requestHandler.addOrigin(origin, repository);
		}
	}

	public synchronized void start() throws IOException {
		if (requestHandler != null)
			throw new IllegalStateException("Stop must be called first");
		requestHandler = new RequestTransactionActor(filter, handler);
		requestTriager = new RequestTriagerActor(filter, requestHandler);
		requestHandler.setErrorPipe(pipeline);
		requestTriager.setErrorPipe(pipeline);
		for (Map.Entry<String, CalliRepository> e : repositories.entrySet()) {
			requestHandler.addOrigin(e.getKey(), e.getValue());
		}
	}

	public synchronized void stop() {
		if (requestTriager != null) {
			requestTriager.shutdown();
			requestHandler.shutdown();
			requestTriager = null;
			requestHandler = null;
		}
	}

	public synchronized boolean isShutdown() {
		if (requestHandler == null)
			return true;
		return requestTriager.isShutdown() && requestHandler.isShutdown();
	}

	public synchronized boolean isTerminated() {
		if (requestHandler == null)
			return true;
		return requestTriager.isTerminated() && requestHandler.isTerminated();
	}

	public synchronized String getErrorPipe() {
		return pipeline.getSystemId();
	}

	public synchronized void setErrorPipe(String url) throws IOException {
		this.pipeline = PipelineFactory.newInstance().createPipeline(url);
	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(
			HttpRequest request, HttpContext ctx) throws HttpException,
			IOException {
		logger.debug("Request received {}", request.getRequestLine());
		InetAddress remoteAddress = getRemoteAddress(ctx);
		Request req = new Request(request, remoteAddress, false);
		final Queue<Exchange> queue = getOrCreateProcessingQueue(ctx);
		Exchange exchange = new Exchange(req, ctx, queue);
		ctx.setAttribute(EXCHANGE_ATTR, exchange);
		submit(exchange);
		return exchange.getConsumer();
	}

	@Override
	public void handle(HttpRequest request, HttpAsyncExchange trigger,
			HttpContext ctx) throws HttpException, IOException {
		logger.debug("Request consumed {}", request.getRequestLine());
		Exchange exchange = (Exchange) ctx.getAttribute(EXCHANGE_ATTR);
		assert exchange != null;
		exchange.setHttpAsyncExchange(trigger);
		ctx.removeAttribute(EXCHANGE_ATTR);
	}

	@Override
	public HttpResponse execute(HttpHost host, HttpRequest request,
			HttpContext ctx) throws IOException {
		logger.debug("Request received {}", request.getRequestLine());
		InetAddress remoteAddress = getRemoteAddress(ctx);
		Request req = new Request(request, remoteAddress, true);
		Exchange exchange = new Exchange(req, ctx);
		HttpAsyncExchange trigger = new ForegroundAsyncExchange(request);
		exchange.setHttpAsyncExchange(trigger);
		execute(exchange);
		return trigger.getResponse();
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return new UnavailableHttpClient().getConnectionManager();
	}

	@Override
	public HttpParams getParams() {
		return new UnavailableHttpClient().getParams();
	}

	public Exchange[] getPendingExchange(HttpContext ctx) {
		Queue<Exchange> queue = (Queue<Exchange>) ctx.getAttribute(PROCESSING_ATTR);
		if (queue == null)
			return null;
		synchronized (queue) {
			return queue.toArray(new Exchange[queue.size()]);
		}
	}

	private void submit(Exchange exchange) {
		RequestTriagerActor requestTriager = getRequestTriager();
		if (requestTriager == null) {
			exchange.submitResponse(new BasicHttpResponse(_503));
		} else {
			requestTriager.submit(exchange);
		}
	}

	private void execute(Exchange exchange) {
		RequestTriagerActor requestTriager = getRequestTriager();
		if (requestTriager == null) {
			exchange.submitResponse(new BasicHttpResponse(_503));
		} else {
			requestTriager.execute(exchange);
		}
	}

	private synchronized RequestTriagerActor getRequestTriager() {
		return requestTriager;
	}

	private Queue<Exchange> getOrCreateProcessingQueue(HttpContext ctx) {
		Queue<Exchange> queue = (Queue<Exchange>) ctx.getAttribute(PROCESSING_ATTR);
		if (queue == null) {
			ctx.setAttribute(PROCESSING_ATTR, queue = new LinkedList<Exchange>());
		}
		return queue;
	}

	private InetAddress getRemoteAddress(HttpContext context) {
		if (context == null)
			return LOCALHOST;
		HttpInetConnection con = (HttpInetConnection) context
				.getAttribute(ExecutionContext.HTTP_CONNECTION);
		if (con == null)
			return LOCALHOST;
		return con.getRemoteAddress();
	}

}