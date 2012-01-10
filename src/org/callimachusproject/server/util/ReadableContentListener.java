/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * Copyright (c) 2011, 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.server.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ContentListener} that can be read using {@link ReadableByteChannel}
 * interface.
 * 
 * @author James Leigh
 * 
 */
public class ReadableContentListener implements ReadableByteChannel,
		ContentListener {
	private static final int DEFAULT_CAPACITY = 8192;
	private Logger logger = LoggerFactory
			.getLogger(ReadableContentListener.class);
	private boolean reading;
	private ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_CAPACITY);
	private volatile boolean closed;
	private volatile boolean completed;

	public boolean isOpen() {
		return !closed;
	}

	public synchronized void finished() {
		debug("finished");
		completed = true;
		notifyAll(); // no more data
	}

	public synchronized void close() throws IOException {
		debug("close");
		closed = true;
		notifyAll(); // consume data
	}

	public synchronized int read(ByteBuffer dst) throws IOException {
		try {
			do {
				if (!reading) {
					buffer.flip();
					reading = true;
				}
				if (buffer.remaining() > 0 || completed)
					break;
				debug("empty");
				wait(); // need content
			} while (true);
		} catch (InterruptedException e) {
			// break
		}
		int remaining = buffer.remaining();
		if (remaining <= 0 && completed) {
			debug("eof");
			return -1;
		} else if (remaining <= dst.remaining()) {
			debug("put");
			dst.put(buffer);
			if (dst.capacity() > buffer.capacity()) {
				// increase capacity
				buffer = ByteBuffer.allocate(dst.capacity());
				reading = false;
			}
			notifyAll(); // will need more content
			return remaining;
		} else if (dst.hasArray()) {
			debug("get");
			int len = dst.remaining();
			buffer.get(dst.array(), dst.arrayOffset() + dst.position(), len);
			dst.position(dst.limit());
			return len;
		} else {
			debug("array");
			int len = dst.remaining();
			byte[] buf = new byte[len];
			buffer.get(buf);
			dst.put(buf);
			return len;
		}
	}

	public synchronized void contentAvailable(ContentDecoder decoder,
			IOControl ioctrl) throws IOException {
		try {
			while (!closed) {
				if (reading) {
					buffer.compact();
					reading = false;
				}
				if (buffer.remaining() > 0)
					break;
				debug("full");
				wait(); // wait until buffer can be read
			}
		} catch (InterruptedException e) {
			// break
		}
		if (closed) {
			debug("consume");
			buffer.clear();
			while (decoder.read(buffer) > 0) {
				buffer.clear();
			}
			if (decoder.isCompleted()) {
				debug("completed");
				completed = true;
			}
		} else {
			debug("contentAvailable");
			int read = decoder.read(buffer);
			if (decoder.isCompleted() || read < 0) {
				debug("completed");
				completed = true;
			}
			notifyAll(); // content available
		}
	}

	private void debug(String msg) {
		if (logger.isDebugEnabled()) {
			logger.debug("{} {}", Thread.currentThread(), msg);
		}
	}

}
