package org.callimachusproject.server.process;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.util.ManagedThreadPool;
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
		super(new ManagedThreadPool(N, N, 0L, TimeUnit.MILLISECONDS, queue,
				"HttpTriage", true), queue);
		this.filter = filter;
		this.handler = handler;
	}

	protected void process(Exchange exchange, boolean foreground)
			throws IOException, OpenRDFException, InterruptedException {
		Request req = exchange.getRequest();
		HttpResponse resp = filter.intercept(req);
		if (resp == null) {
			exchange.setRequest(filter.filter(req));
			if (foreground) {
				handler.execute(exchange);
			} else {
				handler.submit(exchange);
			}
		} else {
			exchange.submitResponse(filter(req, resp));
		}
	}

	protected HttpResponse filter(Request request, HttpResponse response)
			throws IOException {
		HttpResponse resp = filter.filter(request, response);
		HttpEntity entity = resp.getEntity();
		if ("HEAD".equals(request.getMethod()) && entity != null) {
			EntityUtils.consume(entity);
			resp.setEntity(null);
		}
		return resp;
	}

}
