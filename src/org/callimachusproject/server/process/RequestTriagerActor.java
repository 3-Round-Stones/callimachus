package org.callimachusproject.server.process;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.concurrent.ManagedExecutors;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;
import org.openrdf.OpenRDFException;

public class RequestTriagerActor extends ExchangeActor {
	private static final int N = Runtime.getRuntime().availableProcessors();
	private final Filter filter;
	private final RequestTransactionActor handler;

	public RequestTriagerActor(Filter filter, RequestTransactionActor handler) {
		this(new PriorityBlockingQueue<Runnable>(), filter, handler);
	}

	private RequestTriagerActor(BlockingQueue<Runnable> queue, Filter filter,
			RequestTransactionActor handler) {
		super(ManagedExecutors.getInstance().newFixedThreadPool(N, queue, "HttpTriage"),
				queue);
		this.filter = filter;
		this.handler = handler;
	}

	protected void process(Exchange exchange, boolean foreground)
			throws IOException, OpenRDFException, InterruptedException {
		Request req = exchange.getRequest();
		HttpContext context = exchange.getContext();
		HttpResponse resp = filter.intercept(req, context);
		if (resp == null) {
			exchange.setRequest(filter.filter(req, context));
			if (foreground) {
				handler.execute(exchange);
			} else {
				handler.submit(exchange);
			}
		} else {
			exchange.submitResponse(filter(req, context, resp));
		}
	}

	protected HttpResponse filter(Request request, HttpContext context, HttpResponse response)
			throws IOException {
		HttpResponse resp = filter.filter(request, context, response);
		HttpEntity entity = resp.getEntity();
		if ("HEAD".equals(request.getMethod()) && entity != null) {
			EntityUtils.consume(entity);
			resp.setEntity(null);
		}
		return resp;
	}

}
