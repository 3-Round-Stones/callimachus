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
package org.callimachusproject.io;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class AsyncPipe {
	/**
	 * Grace period to wait while the sink buffer is under capacity.
	 */
	private static final int CAPACITY_TIMEOUT = 10 * 1000;

	public abstract static class Sink implements ReadableByteChannel {
		private boolean closed;

		public boolean isOpen() {
			return !closed;
		}

		public void close() throws IOException {
			closed = true;
		}
	};

	abstract static class Source implements WritableByteChannel {
		private boolean closed;

		public boolean isOpen() {
			return !closed;
		}

		public void close() throws IOException {
			closed = true;
		}
	};

	private final ByteBuffer buf;
	private Throwable error;
	private boolean closed;
	private Runnable action;
	boolean stale;
	long expiresAt;

	public AsyncPipe() {
		this(65536);
	}

	public AsyncPipe(int capacity) {
		this.buf = ByteBuffer.allocate(capacity);
		resetTimeout();
	}

	public synchronized boolean isOpen() {
		return !closed;
	}

	/**
	 * If the source pipeline was aborted because the sink buffer was empty past
	 * the grace period.
	 */
	public synchronized boolean isStale() {
		return stale;
	}

	/**
	 * Source operations will no longer see any data (after internal buffer is
	 * consumed or {@link #sink(ReadableByteChannel)} is called) and
	 * {@link #sink(ReadableByteChannel)} will sink to nil.
	 */
	public synchronized void close() {
		closed = true;
		this.notifyAll();
		capacityAvailable();
	}

	/**
	 * Source operations will fail with an IOException with the given throwable as
	 * the cause and the pipe will be closed.
	 */
	public synchronized void fail(Throwable e) {
		error = e;
		close();
	}

	public synchronized boolean hasAvailableCapacity() {
		if (closed || buf.hasRemaining())
			return true;
		try {
			this.wait(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return closed || buf.hasRemaining();
	}

	public synchronized void onAvailableCapacity(Runnable action) {
		if (closed || buf.hasRemaining()) {
			action.run();
		} else {
			this.action = action;
		}
	}

	/**
	 * Read n bytes from in into the internal buffer
	 */
	public synchronized int sink(ReadableByteChannel in) throws IOException {
		if (closed) {
			buf.clear();
		}
		try {
			return in.read(buf);
		} catch (Error e) {
			fail(e);
			throw e;
		} catch (IOException e) {
			fail(e);
			throw e;
		} catch (RuntimeException e) {
			fail(e);
			throw e;
		} finally {
			this.notifyAll();
		}
	}

	/**
	 * Blocking read bytes from the internal buffer
	 */
	public synchronized ReadableByteChannel source() {
		return new ReadableByteChannel() {
			private boolean closed;

			public boolean isOpen() {
				return !closed;
			}

			public void close() throws IOException {
				closed = true;
				abort();
			}

			public int read(final ByteBuffer dst) throws IOException {
				synchronized (AsyncPipe.this) {
					int n = 0;
					while (n == 0 && dst.hasRemaining()) {
						n = source(new Source() {
							public int write(ByteBuffer src) throws IOException {
								return copy(src, dst);
							}
						});
						if (!dst.hasRemaining()) {
							resetTimeout();
						} else if (n == 0) {
							try {
								long timeout = expiresAt - System.currentTimeMillis();
								if (timeout <= 0) {
									stale = true;
									close();
									throw new InterruptedIOException("Read timeout");
								}
								AsyncPipe.this.wait(timeout);
							} catch (InterruptedException e) {
								InterruptedIOException ie = new InterruptedIOException(
										e.toString());
								ie.initCause(e);
								throw ie;
							}
						}
					}
					return n;
				}
			}

			private int copy(ByteBuffer src, ByteBuffer dst) {
				int n = src.remaining();
				int limit = dst.remaining();
				if (n <= limit) {
					dst.put(src);
				} else {
					n = limit;
					for (int i = 0; i < n; i++) {
						dst.put(src.get());
					}
				}
				return n;
			}
		};
	}

	synchronized void abort() {
		close();
		buf.clear();
		capacityAvailable();
	}

	/**
	 * Write n bytes to out from the internal buffer
	 */
	synchronized int source(WritableByteChannel out) throws IOException {
		if (error != null) {
			try {
				throw new IOException(error);
			} finally {
				error = null;
			}
		}
		if (closed && buf.position() == 0)
			return -1;
		buf.flip();
		int n = out.write(buf);
		buf.compact();
		if (buf.remaining() >= buf.capacity() / 2) {
			capacityAvailable();
		}
		return n;
	}

	private synchronized void capacityAvailable() {
		if (action != null) {
			try {
				action.run();
			} finally {
				action = null;
			}
		}
	}

	private synchronized void resetTimeout() {
		this.expiresAt = System.currentTimeMillis() + CAPACITY_TIMEOUT;
	}

}
