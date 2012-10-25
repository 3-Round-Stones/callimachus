/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.callimachusproject.server.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncRequestConsumer;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.client.HttpEntityWrapper;
import org.callimachusproject.server.util.ChannelUtil;

/**
 * Pipes incoming request body to HttpEntity content source
 * 
 * @author James Leigh
 * 
 */
public class ConsumingHttpEntity extends
		AbstractAsyncRequestConsumer<Request> {
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

	private Request request;
	private Pipe pipe;
	private ByteBuffer buf;
	private Throwable throwable;

	public ConsumingHttpEntity(Request request) throws IOException {
		this.request = request;
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest ereq = (HttpEntityEnclosingRequest) request;
			HttpEntity entity = ereq.getEntity();
			if (entity == null)
				return;
			pipe = Pipe.open();
			buf = ByteBuffer.allocate(4096);
			final SourceChannel source = pipe.source();
			request.setEntity(new HttpEntityWrapper(entity) {
				protected InputStream getDelegateContent() throws IOException {
					return ChannelUtil
							.newInputStream(new ReadableSource(source));
				}
			});
		}
	}

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
		return this.request;
	}

	@Override
	protected void releaseResources() {
		this.request = null;
		this.buf = null;
		this.pipe = null;
		this.throwable = null;
	}

	void verify() throws IOException {
		Exception exception = super.getException();
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

}
