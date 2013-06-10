/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.chain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.callimachusproject.client.StreamingHttpEntity;
import org.callimachusproject.io.ChannelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures the message body matches the Content-MD5 header.
 */
public class MD5ValidationEntity extends StreamingHttpEntity {
	private String md5;

	public MD5ValidationEntity(HttpEntity entity, String md5) {
		super(entity);
		this.md5 = md5;
	}

	public String getContentMD5() {
		return md5;
	}

	@Override
	protected InputStream getDelegateContent() throws IOException,
			IllegalStateException {
		InputStream delegate = super.getDelegateContent();
		try {
			ReadableByteChannel in = ChannelUtil.newChannel(delegate);
			return ChannelUtil
					.newInputStream(new MD5ValidatingChannel(in, md5) {
						public void close() throws IOException {
							super.close();
							md5 = getContentMD5();
						}
					});
		} catch (NoSuchAlgorithmException e) {
			Logger logger = LoggerFactory.getLogger(MD5ValidationEntity.class);
			logger.warn(e.getMessage(), e);
			return delegate;
		}
	}

	/**
	 * Computes the MD5 sum of this stream and throws an exception if it is
	 * wrong.
	 */
	static class MD5ValidatingChannel implements ReadableByteChannel {
		private final ReadableByteChannel delegate;
		private final String md5;
		private final MessageDigest digest;
		private boolean closed;
		private String contentMD5;

		public MD5ValidatingChannel(ReadableByteChannel delegate, String md5)
				throws NoSuchAlgorithmException {
			this.delegate = delegate;
			this.md5 = md5;
			digest = MessageDigest.getInstance("MD5");
		}

		public String getContentMD5() {
			if (closed)
				return contentMD5;
			return md5;
		}

		public boolean isOpen() {
			return delegate.isOpen();
		}

		public void close() throws IOException {
			if (!closed) {
				closed = true;
				delegate.close();
				byte[] hash = Base64.encodeBase64(digest.digest());
				contentMD5 = new String(hash, "UTF-8");
				if (md5 != null && !md5.equals(contentMD5)) {
					throw new IOException(
							"Content-MD5 header does not match message body");
				}
			}
		}

		public int read(ByteBuffer dst) throws IOException {
			int read = delegate.read(dst);
			if (read > 0) {
				int limit = dst.limit();
				dst.flip();
				digest.update(dst);
				dst.limit(limit);
			}
			return read;
		}

		public String toString() {
			return delegate.toString();
		}

	}
}
