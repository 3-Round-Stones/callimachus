package org.callimachusproject.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class AutoCloseChannel implements ReadableByteChannel {
	private final ReadableByteChannel delegate;
	private boolean closed;

	public AutoCloseChannel(ReadableByteChannel delegate) {
		this.delegate = delegate;
	}

	public String toString() {
		return delegate.toString();
	}

	public boolean isOpen() {
		return delegate.isOpen();
	}

	public void close() throws IOException {
		delegate.close();
	}

	public final int read(ByteBuffer dst) throws IOException {
		if (closed)
			return -1;
		int read = delegate.read(dst);
		if (read < 0) {
			closed = true;
			close();
		}
		return read;
	}
}
