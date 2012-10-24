/*
 * Copyright 2010, Zehperia LLC Some rights reserved.
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
package org.callimachusproject.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.server.util.ChannelUtil;

/**
 * Implements the ProducingNHttpEntity interface for subclasses.
 * 
 * @author James Leigh
 * 
 */
public class HttpEntityWrapper implements HttpAsyncContentProducer, HttpEntity {
	private HttpEntity entity;
	private InputStream in;
	private ReadableByteChannel cin;
	private ByteBuffer buf;
	private boolean chunked;
	private Header contentType;
	private long contentLength;
	private Header contentEncoding;

	public HttpEntityWrapper(HttpEntity entity) {
		assert entity != null;
		this.entity = entity;
		chunked = entity.isChunked();
		contentType = entity.getContentType();
		contentLength = entity.getContentLength();
		contentEncoding = entity.getContentEncoding();
	}

	public HttpEntity getEntityDelegate() {
		if (entity == null)
			throw new IllegalStateException("Entity has already been consumed");
		return entity;
	}

	@Override
	public String toString() {
		if (entity == null)
			return "null";
		return entity.toString();
	}

	public Header getContentEncoding() {
		return contentEncoding;
	}

	public long getContentLength() {
		return contentLength;
	}

	public Header getContentType() {
		return contentType;
	}

	public boolean isChunked() {
		return chunked;
	}

	public final boolean isRepeatable() {
		return false;
	}

	public final boolean isStreaming() {
		return entity != null && entity.isStreaming();
	}

	public final void consumeContent() throws IOException {
		close();
	}

	public final synchronized InputStream getContent() throws IOException, IllegalStateException {
		if (in != null)
			return in;
		final InputStream stream = getDelegateContent();
		if (ChannelUtil.isChannel(stream)) {
			final ReadableByteChannel delegate = ChannelUtil.newChannel(stream);
			return in = ChannelUtil.newInputStream(new ReadableByteChannel() {

				public boolean isOpen() {
					return delegate.isOpen();
				}

				public void close() throws IOException {
					try {
						delegate.close();
					} finally {
						closeEntity();
					}
				}

				public int read(ByteBuffer dst) throws IOException {
					return delegate.read(dst);
				}
			});
		} else {
			return in = new FilterInputStream(stream) {
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						closeEntity();
					}
				}
			};
		}
	}

	public void writeTo(OutputStream out) throws IOException {
		InputStream in = getContent();
		try {
			int l;
			byte[] buf = new byte[2048];
			while ((l = in.read(buf)) != -1) {
				out.write(buf, 0, l);
			}
		} finally {
			in.close();
		}
	}

	@Override
	public final void close() throws IOException {
		try {
			closeEntity();
		} finally {
			if (cin != null) {
				cin.close();
				cin = null;
				buf = null;
			}
		}
	}

	@Override
	public final synchronized void produceContent(ContentEncoder encoder, IOControl ioctrl)
			throws IOException {
		if (cin == null) {
			cin = ChannelUtil.newChannel(getContent());
			buf = ByteBuffer.allocate(1024);
		}
		if (cin.read(buf) < 0 && buf.position() == 0) {
			encoder.complete();
		} else {
			buf.flip();
			encoder.write(buf);
			buf.compact();
		}

	}

	protected InputStream getDelegateContent() throws IOException {
		return getEntityDelegate().getContent();
	}

	final void closeEntity() throws IOException {
		try {
			if (entity instanceof HttpAsyncContentProducer) {
				((HttpAsyncContentProducer) entity).close();
			} else if (entity != null) {
				EntityUtils.consume(entity);
			}
		} finally {
			entity = null;
		}
	}

}
