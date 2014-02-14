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
