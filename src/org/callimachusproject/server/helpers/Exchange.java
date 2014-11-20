/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
import org.apache.http.nio.IOControl;
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
	private HttpAsyncResponseProducer producer;
	private int timeout = -1;
	private boolean expectContinue;
	private HttpAsyncResponseProducer submitContinue;
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
		if (expectContinue && response != null) {
			this.submitContinue = new LoggingResponseProducer(response);
			if (exchange != null) {
				exchange.submitResponse(submitContinue);
			}
		}
	}

	public synchronized HttpAsyncExchange getHttpAsyncExchange() {
		return exchange;
	}

	public synchronized void setHttpAsyncExchange(HttpAsyncExchange exchange) {
		assert exchange != null;
		this.exchange = exchange;
		exchange.setCallback(this);
		if (timeout != -1) {
			exchange.setTimeout(timeout);
		}
		if (response != null) {
			exchange.submitResponse(producer = new LoggingResponseProducer(response));
		} else if (submitContinue != null) {
			exchange.submitResponse(submitContinue);
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
		closeRequest();
		assert response != null;
		if (this.response != null && this.exchange != null) {
			consume(response); // too late! already committed a response
		} else if (this.response != null) {
			consume(this.response); // discard the previous response
			this.response = response;
		} else if (exchange != null) {
			this.response = response;
			exchange.submitResponse(producer = new LoggingResponseProducer(response));
		} else {
			this.response = response;
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

}
