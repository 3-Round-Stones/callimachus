package org.callimachusproject.server.process;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.server.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exchange implements Cancellable {
	final Logger logger = LoggerFactory.getLogger(Exchange.class);
	private static final BasicHttpResponse _100 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 100, "Continue");
	private final CountDownLatch latch = new CountDownLatch(1);
	private Request request;
	private final HttpContext context;
	private final Queue<Exchange> queue;
	private HttpAsyncExchange exchange;
	private HttpResponse response;
	private int timeout = -1;
	private boolean submitContinue;
	private boolean cancelled;

	public Exchange(Request request, HttpContext context) {
		this(request, context, null);
	}

	public Exchange(Request request, HttpContext context, Queue<Exchange> queue) {
		assert request != null;
		this.request = request;
		this.context = context;
		this.queue = queue;
		if (queue != null) {
			synchronized (queue) {
				queue.add(this);
			}
		}
	}

	public synchronized Request getRequest() {
		return request;
	}

	public synchronized void setRequest(Request request) {
		this.request = request;
	}

	public HttpContext getContext() {
		return context;
	}

	public void awaitVerification(int timeout, TimeUnit unit) throws InterruptedException {
		latch.await(timeout, unit);
	}

	public synchronized void verified() {
		latch.countDown();
		String expect = request.getHeader("Expect");
		submitContinue = expect != null && expect.contains("100-continue");
	}

	public synchronized HttpAsyncExchange getHttpAsyncExchange() {
		return exchange;
	}

	public synchronized void setHttpAsyncExchange(HttpAsyncExchange exchange) {
		this.exchange = exchange;
		exchange.setCallback(this);
		if (timeout != -1) {
			exchange.setTimeout(timeout);
		}
		if (response != null) {
			exchange.submitResponse(new BasicAsyncResponseProducer(response));
		} else if (submitContinue) {
			exchange.submitResponse(new BasicAsyncResponseProducer(_100));
		}
	}

	public synchronized boolean isPendingVerification() {
		String expect = request.getHeader("Expect");
		return !submitContinue && expect != null && expect.contains("100-continue");
	}

	public synchronized boolean isReadingRequest() {
		return exchange == null;
	}

	public synchronized boolean isPendingResponse() {
		return response == null;
	}

	public synchronized boolean isCancelled() {
		return cancelled;
	}

	public synchronized boolean isCompleted() {
		return exchange != null && exchange.isCompleted();
	}

	public synchronized void setTimeout(int timeout) {
		this.timeout = timeout;
		if (exchange != null) {
			exchange.setTimeout(timeout);
		}
	}

	public synchronized int getTimeout() {
		if (exchange == null)
			return timeout;
		return exchange.getTimeout();
	}

	public synchronized void submitResponse(HttpResponse response) {
		closeRequest();
		assert response != null;
		if (this.response == null) {
			this.response = response;
			if (exchange != null) {
				exchange.submitResponse(new BasicAsyncResponseProducer(response));
			}
		} else if (exchange == null) {
			try {
				consume(this.response);
			} finally {
				this.response = response;
			}
		} else {
			consume(response);
		}
	}

	@Override
	public synchronized boolean cancel() {
		cancelled = true;
		closeRequest();
		return false;
	}

	synchronized void closeRequest() {
		latch.countDown();
		request.closeRequest();
		if (queue != null) {
			synchronized (queue) {
				queue.remove(this);
			}
		}
	}

	private void consume(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			EntityUtils.consumeQuietly(entity);
		}
	}

}
