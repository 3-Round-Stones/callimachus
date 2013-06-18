package org.callimachusproject.server.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Queue;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.EntityAsyncContentProducer;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.StreamingHttpEntity;
import org.callimachusproject.io.AsyncPipe;
import org.callimachusproject.io.ChannelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exchange implements Cancellable {
	final Logger logger = LoggerFactory.getLogger(Exchange.class);
	private Request request;
	private final Queue<Exchange> queue;
	private final Consumer consumer;
	private HttpAsyncExchange exchange;
	private HttpResponse response;
	private HttpAsyncContentProducer producer;
	private int timeout = -1;
	private boolean expectContinue;
	private HttpResponse submitContinue;
	private boolean ready;
	private boolean cancelled;

	public Exchange(Request request, Queue<Exchange> queue)
			throws IOException {
		assert request != null;
		assert queue != null;
		this.request = request;
		this.queue = queue;
		Header expect = request.getFirstHeader("Expect");
		setExpectContinue(expect != null
				&& expect.getValue().equalsIgnoreCase("100-continue"));
		synchronized (queue) {
			queue.add(this);
		}
		consumer = new Consumer(request);
	}

	public HttpAsyncRequestConsumer<HttpRequest> getConsumer() {
		return consumer;
	}

	public String toString() {
		return getRequest().toString();
	}

	public synchronized Request getRequest() {
		return request;
	}

	public synchronized void setRequest(Request request) {
		this.request = request;
	}

	public synchronized void submitContinue(HttpResponse response) {
		setSubmitContinue(response);
		if (expectContinue && exchange != null) {
			exchange.submitResponse(new Producer());
			ready = true;
		}
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
			closeRequest();
		}
		if (response != null || getSubmitContinue() != null) {
			exchange.submitResponse(new Producer());
			ready = true;
		}
	}

	public synchronized boolean isPendingVerification() {
		return submitContinue == null && expectContinue;
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
		if (exchange != null) {
			closeRequest();
		}
		assert response != null;
		if (this.response != null && this.exchange != null) {
			consume(response);
		} else {
			if (this.response != null) {
				consume(this.response);
			}
			this.response = response;
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				if (entity instanceof HttpAsyncContentProducer) {
					this.producer = (HttpAsyncContentProducer) entity;
				} else {
					this.producer = new EntityAsyncContentProducer(entity);
				}
			}
			notifyAll();
			if (exchange != null && !ready) {
				exchange.submitResponse(new Producer());
				ready = true;
			}
		}
	}

	@Override
	public synchronized boolean cancel() {
		cancelled = true;
		closeRequest();
		if (producer != null) {
			try {
				producer.close();
			} catch (IOException e) {
				logger.debug(e.toString(), e);
			}
		}
		return false;
	}

	synchronized void closeRequest() {
		if (consumer != null) {
			consumer.close();
		}
		EntityUtils.consumeQuietly(request.getEntity());
		if (queue != null) {
			synchronized (queue) {
				queue.remove(this);
			}
		}
	}

	synchronized void setExpectContinue(boolean expectContinue) {
		this.expectContinue = expectContinue;
	}

	private synchronized HttpResponse getSubmitContinue() {
		if (expectContinue)
			return submitContinue;
		return null;
	}

	private synchronized void setSubmitContinue(HttpResponse submitContinue) {
		this.submitContinue = submitContinue;
	}

	private void consume(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			EntityUtils.consumeQuietly(entity);
		}
	}

	private class Consumer implements HttpAsyncRequestConsumer<HttpRequest> {
		private final HttpRequest request;
		private AsyncPipe pipe;
		private Exception ex;

		public Consumer(HttpRequest request) {
			this(request, 65536);
		}

		public Consumer(HttpRequest request, int capacity) {
			this.request = request;
			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest ereq = (HttpEntityEnclosingRequest) request;
				HttpEntity entity = ereq.getEntity();
				if (entity == null) {
					pipe = null;
				} else {
					pipe = new AsyncPipe(capacity);
					entity = new StreamingHttpEntity(entity) {
						protected InputStream getDelegateContent()
								throws IOException {
							return ChannelUtil.newInputStream(pipe.source());
						}
					};
					ereq.setEntity(entity);
				}
			} else {
				pipe = null;
			}
		}

		@Override
		public void requestReceived(HttpRequest request) throws HttpException,
				IOException {
		}

		@Override
		public void consumeContent(final ContentDecoder decoder,
				final IOControl ioctrl) throws IOException {
			setExpectContinue(false);
			assert pipe != null;
			pipe.sink(new AsyncPipe.Sink() {
				public int read(ByteBuffer dst) throws IOException {
					return decoder.read(dst);
				}
			});
			if (decoder.isCompleted()) {
				pipe.close();
			} else if (!pipe.hasAvailableCapacity()) {
				synchronized (pipe) {
					if (!pipe.hasAvailableCapacity()) {
						logger.info("Suspend {}", request.getRequestLine());
						ioctrl.suspendInput();
						pipe.onAvailableCapacity(new Runnable() {
							public void run() {
								logger.info("Resume {}", request.getRequestLine());
								ioctrl.requestInput();
							}
						});
					}
				}
			}
		}

		@Override
		public void close() {
			if (pipe != null) {
				pipe.close();
			}
		}

		@Override
		public void requestCompleted(HttpContext context) {
			close();
		}

		@Override
		public void failed(Exception ex) {
			this.ex = ex;
			if (pipe != null) {
				pipe.fail(ex);
			}
		}

		@Override
		public Exception getException() {
			return ex;
		}

		@Override
		public HttpRequest getResult() {
			return request;
		}

		@Override
		public boolean isDone() {
			return pipe == null || !pipe.isOpen();
		}

		public String toString() {
			return String.valueOf(request);
		}
	}

	private class Producer implements HttpAsyncResponseProducer {

		@Override
		public synchronized HttpResponse generateResponse() {
			while (response == null) {
				HttpResponse _100 = getSubmitContinue();
				if (_100 != null)
					return _100;
				try {
					wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			return response;
		}

		@Override
		public synchronized void produceContent(ContentEncoder encoder,
				IOControl ioctrl) throws IOException {
			if (producer != null) {
				producer.produceContent(encoder, ioctrl);
				if (encoder.isCompleted()) {
					close();
				}
			}
		}

		@Override
		public void responseCompleted(HttpContext context) {
			logger.trace("Response completed: {}", context);
		}

		@Override
		public void failed(Exception ex) {
			logger.warn(ex.toString());
		}

		@Override
		public void close() throws IOException {
			if (producer != null) {
				producer.close();
			}
		}

		public String toString() {
			return String.valueOf(response);
		}
	}

}
