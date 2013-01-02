package org.callimachusproject.server.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class AsyncPipe {
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

	public AsyncPipe() {
		this(65536);
	}

	public AsyncPipe(int capacity) {
		this.buf = ByteBuffer.allocate(capacity);
	}

	public synchronized boolean isOpen() {
		return !closed;
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
					while (n == 0) {
						n = source(new Source() {
							public int write(ByteBuffer src) throws IOException {
								return copy(src, dst);
							}
						});
						if (n == 0) {
							try {
								AsyncPipe.this.wait();
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

}
