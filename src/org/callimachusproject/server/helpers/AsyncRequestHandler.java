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
package org.callimachusproject.server.helpers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Queue;

import org.apache.http.HttpConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerMapper;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the HTTP requests.
 * 
 * @author James Leigh
 * 
 */
public class AsyncRequestHandler implements HttpAsyncRequestHandlerMapper,
		HttpAsyncRequestHandler<HttpRequest> {
	private static final InetAddress LOCALHOST = DomainNameSystemResolver
			.getInstance().getLocalHost();

	private final Logger logger = LoggerFactory
			.getLogger(AsyncRequestHandler.class);
	private final AsyncExecChain handler;

	public AsyncRequestHandler(AsyncExecChain handler) {
		this.handler = handler;
	}

	@Override
	public HttpAsyncRequestHandler<?> lookup(HttpRequest request) {
		return this;
	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(
			HttpRequest request, HttpContext context) throws HttpException,
			IOException {
		logger.debug("Request received {}", request.getRequestLine());
		CalliContext cc = CalliContext.adapt(context);
		final Request req = new Request(request);
		final Queue<Exchange> queue = cc.getOrCreateProcessingQueue();
		final Exchange exchange = new Exchange(req, queue);
		cc.setExchange(exchange);
		handle(req, context, exchange);
		return exchange.getConsumer();
	}

	@Override
	public void handle(HttpRequest request, HttpAsyncExchange trigger,
			HttpContext context) throws HttpException, IOException {
		logger.debug("Request consumed {}", request.getRequestLine());
		CalliContext ctx = CalliContext.adapt(context);
		Exchange exchange = ctx.getExchange();
		assert exchange != null;
		exchange.setHttpAsyncExchange(trigger);
		ctx.setExchange(null);
	}

	private void handle(final Request req, HttpContext context,
			final Exchange exchange) {
		InetAddress remoteAddress = getRemoteAddress(context);
		// fork HttpContext so it can be used in other threads
		HttpContext ctx = new BasicHttpContext(context);
		CalliContext cc = CalliContext.adapt(ctx);
		cc.setReceivedOn(System.currentTimeMillis());
		cc.setClientAddr(remoteAddress);
		HttpHost host = URIUtils.extractHost(URI.create(req.getOrigin() + "/"));
		HttpRoute route = new HttpRoute(host);
		HttpExecutionAware execAware = null;
		try {
			handler.execute(route, HttpRequestWrapper.wrap(req), ctx,
					execAware, new FutureCallback<CloseableHttpResponse>() {
						public void completed(CloseableHttpResponse result) {
							exchange.submitResponse(result);
						}

						public void failed(Exception ex) {
							logger.error(ex.toString(), ex);
							ResponseException e;
							if (ex instanceof ResponseException) {
								e = (ResponseException) ex;
							} else {
								e = new InternalServerError(ex);
							}
							HttpUriResponse result = new ResponseBuilder(req)
									.exception(e);
							exchange.submitResponse(result);
						}

						public void cancelled() {
							exchange.cancel();
						}
					});
		} catch (ResponseException ex) {
			logger.error(ex.toString(), ex);
			HttpUriResponse result = new ResponseBuilder(req).exception(ex);
			exchange.submitResponse(result);
		} catch (Exception ex) {
			logger.error(ex.toString(), ex);
			HttpUriResponse result = new ResponseBuilder(req)
					.exception(new InternalServerError(ex));
			exchange.submitResponse(result);
		}
	}

	private InetAddress getRemoteAddress(HttpContext context) {
		if (context == null)
			return LOCALHOST;
		HttpConnection con = HttpCoreContext.adapt(context).getConnection();
		if (con instanceof HttpInetConnection)
			return ((HttpInetConnection) con).getRemoteAddress();
		return LOCALHOST;
	}

}