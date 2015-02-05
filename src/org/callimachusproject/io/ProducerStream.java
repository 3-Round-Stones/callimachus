/*
 * Copyright (c) 2011-2014, 3 Round Stones Inc. Some rights reserved.
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
package org.callimachusproject.io;

import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.callimachusproject.concurrent.ManagedExecutors;

/**
 * A Piped {@link InputStream} that runs the producer in another thread. Both
 * sides of the pipe are buffered to reduce latency.
 * 
 * @author James Leigh
 * 
 */
public class ProducerStream extends FilterInputStream {
	private static final int PACKET_SIZE = 1500;

	public interface OutputProducer {
		void produce(OutputStream out) throws IOException;
	}

	private static ExecutorService executor = ManagedExecutors.getInstance()
			.getProducerThreadPool();
	private final OutputProducer producer;
	private final Future<Void> task;
	private final CountDownLatch started = new CountDownLatch(1);
	private final CountDownLatch stopped = new CountDownLatch(1);
	private Throwable throwable;

	public ProducerStream(OutputProducer producer) throws IOException {
		this(producer, PACKET_SIZE);
	}

	public ProducerStream(final OutputProducer producer, int size) throws IOException {
		super(null);
		this.producer = producer;
		PipedInputStream in = new PipedInputStream();
		PipedOutputStream out = new PipedOutputStream(in);
		this.in = new LatencyInputStream(in, size);
		final BufferedOutputStream sink = new BufferedOutputStream(out, size);
		task = executor.submit(new Runnable() {
			public void run() {
				try {
					started.countDown();
					producer.produce(sink);
				} catch (InterruptedIOException e) {
					// exit
				} catch (ClosedChannelException e) {
					// exit
				} catch (IOException e) {
					if (!"Broken pipe".equals(e.getMessage())) {
						throwable = e;
					}
				} catch (RuntimeException e) {
					throwable = e;
				} catch (Error e) {
					throwable = e;
				} finally {
					try {
						sink.close();
					} catch (InterruptedIOException e) {
						// exit
					} catch (ClosedChannelException e) {
						// exit
					} catch (IOException e) {
						throwable = throwable == null ? e : throwable;
					} catch (RuntimeException e) {
						throwable = throwable == null ? e : throwable;
					} catch (Error e) {
						throwable = throwable == null ? e : throwable;
					} finally {
						stopped.countDown();
					}
				}
			}

			public String toString() {
				return producer.toString();
			}
		}, null);
	}

	public String toString() {
		return producer.toString();
	}

	public void close() throws IOException {
		try {
			verify();
		} finally {
			super.close();
		}
		verify();
		try {
			// task enters producer try/finally block
			started.await();
			// send interrupt
			task.cancel(true);
			// task exits the finally block
			stopped.await();
		} catch (InterruptedException e) {
			InterruptedIOException exc;
			exc = new InterruptedIOException(e.toString());
			exc.initCause(e);
			throw exc;
		}
	}

	@Override
	public int read() throws IOException {
		verify();
		return super.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		verify();
		return super.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		verify();
		return super.skip(n);
	}

	@Override
	public int available() throws IOException {
		verify();
		return super.available();
	}

	@Override
	public synchronized void reset() throws IOException {
		verify();
		super.reset();
	}

	private void verify() throws IOException {
		try {
			if (throwable instanceof Error)
				throw (Error) throwable;
			if (throwable instanceof IOException)
				throw new IOException(throwable);
			if (throwable != null)
				throw new IOException(throwable);
		} finally {
			throwable = null;
		}
	}

}
