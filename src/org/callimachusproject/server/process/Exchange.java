package org.callimachusproject.server.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.EntityAsyncContentProducer;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.protocol.AbstractAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.HttpEntityWrapper;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.util.ChannelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exchange implements Cancellable {
	final Logger logger = LoggerFactory.getLogger(Exchange.class);
	private static final BasicHttpResponse _100 = new BasicHttpResponse(
			HttpVersion.HTTP_1_1, 100, "Continue");
	private Request request;
	private final HttpContext context;
	private final Queue<Exchange> queue;
	private HttpAsyncExchange exchange;
	private HttpResponse response;
	private HttpAsyncContentProducer producer;
	private int timeout = -1;
	private boolean expectContinue;
	private boolean submitContinue;
	private boolean ready;
	private boolean cancelled;
	private Consumer consumer;
	private Pipe pipe;
	private ByteBuffer buf;
	private Throwable throwable;

	public Exchange(Request request, HttpContext context) throws IOException {
		this(request, context, null);
	}

	public Exchange(Request request, HttpContext context, Queue<Exchange> queue)
			throws IOException {
		assert request != null;
		this.request = request;
		this.context = context;
		this.queue = queue;
		String expect = request.getHeader("Expect");
		setExpectContinue(expect != null
				&& expect.equalsIgnoreCase("100-continue"));
		if (queue != null) {
			synchronized (queue) {
				queue.add(this);
			}
		}
	}

	public synchronized HttpAsyncRequestConsumer<Request> getConsumer()
			throws IOException {
		if (consumer == null) {
			consumer = new Consumer();
			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest ereq = (HttpEntityEnclosingRequest) request;
				HttpEntity entity = ereq.getEntity();
				if (entity == null)
					return consumer;
				pipe = Pipe.open();
				buf = ByteBuffer.allocate(4096);
				final SourceChannel source = pipe.source();
				request.setEntity(new HttpEntityWrapper(entity) {
					protected InputStream getDelegateContent()
							throws IOException {
						return ChannelUtil.newInputStream(new ReadableSource(
								source));
					}
				});
			}
		}
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

	public HttpContext getContext() {
		return context;
	}

	public synchronized void verified(String credential) {
		String expect = request.getHeader("Expect");
		submitContinue = expect != null
				&& expect.equalsIgnoreCase("100-continue");
		if (isSubmitContinue() && exchange != null) {
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
		if (response != null || isSubmitContinue()) {
			exchange.submitResponse(new Producer());
			ready = true;
		}
	}

	public synchronized boolean isPendingVerification() {
		return !submitContinue && expectContinue;
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
		return false;
	}

	synchronized void closeRequest() {
		request.closeRequest();
		if (queue != null) {
			synchronized (queue) {
				queue.remove(this);
			}
		}
	}

	void verify() throws IOException {
		Exception exception = consumer.getException();
		if (exception != null)
			throw new IOException(exception);
		try {
			if (throwable instanceof Error)
				throw (Error) throwable;
			if (throwable != null)
				throw new IOException(throwable);
		} finally {
			throwable = null;
		}
	}

	synchronized void setExpectContinue(boolean expectContinue) {
		this.expectContinue = expectContinue;
	}

	private synchronized boolean isSubmitContinue() {
		return submitContinue && expectContinue;
	}

	private void consume(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			EntityUtils.consumeQuietly(entity);
		}
	}

	private class ReadableSource implements ReadableByteChannel {
		private ReadableByteChannel ch;

		public ReadableSource(ReadableByteChannel ch) {
			this.ch = ch;
		}

		public boolean isOpen() {
			return ch.isOpen();
		}

		public void close() throws IOException {
			try {
				verify();
			} finally {
				ch.close();
			}
			verify();
		}

		public int read(ByteBuffer dst) throws IOException {
			verify();
			return ch.read(dst);
		}
	}

	private class Consumer extends AbstractAsyncRequestConsumer<Request> {

		@Override
		protected synchronized void onRequestReceived(final HttpRequest req)
				throws IOException {
		}

		@Override
		protected synchronized void onEntityEnclosed(final HttpEntity entity,
				final ContentType contentType) {
		}

		@Override
		protected synchronized void onContentReceived(final ContentDecoder in,
				final IOControl ioctrl) throws IOException {
			setExpectContinue(false);
			assert pipe != null;
			while (in.read(buf) >= 0 || buf.position() != 0) {
				try {
					if (pipe.source().isOpen()) {
						buf.flip();
						pipe.sink().write(buf);
						buf.compact();
					} else {
						buf.clear();
					}
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
					return;
				} catch (ClosedChannelException e) {
					// exit
				} catch (IOException e) {
					throwable = e;
				} catch (RuntimeException e) {
					throwable = e;
				} catch (Error e) {
					throwable = e;
				}
			}
		}

		@Override
		protected Request buildResult(final HttpContext context) {
			return request;
		}

		@Override
		protected void releaseResources() {
			try {
				if (pipe != null) {
					pipe.sink().close();
				}
			} catch (InterruptedIOException e) {
				Thread.currentThread().interrupt();
			} catch (ClosedChannelException e) {
				// exit
			} catch (IOException e) {
				throwable = throwable == null ? e : throwable;
			} catch (RuntimeException e) {
				throwable = throwable == null ? e : throwable;
			} catch (Error e) {
				throwable = throwable == null ? e : throwable;
			}
		}
	}

	private class Producer implements HttpAsyncResponseProducer {

		@Override
		public synchronized HttpResponse generateResponse() {
			while (response == null) {
				if (isSubmitContinue())
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
			logger.error(ex.toString(), ex);
		}

		@Override
		public synchronized void close() throws IOException {
			if (producer != null) {
				producer.close();
			}
		}
	}

}
