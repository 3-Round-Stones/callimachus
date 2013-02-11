/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Utilities methods for converting between channels and streams.
 * 
 * @author James Leigh
 * 
 */
public final class ChannelUtil {

	public static boolean isChannel(InputStream in) {
		return in instanceof ChannelInputStream
				&& ChannelInputStream.class.equals(in.getClass())
				|| in instanceof FileInputStream
				&& FileInputStream.class.equals(in.getClass());
	}

	public static ReadableByteChannel newChannel(InputStream in) {
		if (in == null)
			return null;
		if (in instanceof ChannelInputStream)
			return ((ChannelInputStream) in).getChannel();
		return Channels.newChannel(in);
	}

	public static WritableByteChannel newChannel(OutputStream out) {
		if (out == null)
			return null;
		return Channels.newChannel(out);
	}

	public static ReadableByteChannel newChannel(byte[] bytes) {
		if (bytes == null)
			return null;
		return Channels.newChannel(new ByteArrayInputStream(bytes));
	}

	public static InputStream newInputStream(ReadableByteChannel ch) {
		if (ch == null)
			return null;
		return new ChannelInputStream(ch);
	}

	public static OutputStream newOutputStream(WritableByteChannel ch) {
		if (ch == null)
			return null;
		return Channels.newOutputStream(ch);
	}

	public static BufferedReader newReader(ReadableByteChannel ch, Charset cs) {
		if (ch == null)
			return null;
		return new BufferedReader(Channels.newReader(ch, cs.newDecoder(), -1));
	}

	public static Writer newWriter(WritableByteChannel ch, Charset cs) {
		if (ch == null)
			return null;
		return Channels.newWriter(ch, cs.newEncoder(), -1);
	}

	public static long transfer(InputStream in, OutputStream out)
			throws IOException {
		return transfer(newChannel(in), newChannel(out), null);
	}

	public static long transfer(InputStream in, WritableByteChannel out)
			throws IOException {
		return transfer(newChannel(in), out, null);
	}

	public static long transfer(ReadableByteChannel in, OutputStream out)
			throws IOException {
		return transfer(in, newChannel(out), null);
	}

	public static long transfer(ReadableByteChannel in, WritableByteChannel out)
			throws IOException {
		return transfer(in, out, null);
	}

	public static long transfer(InputStream in, OutputStream out,
			MessageDigest digest) throws IOException {
		return transfer(newChannel(in), out, digest);
	}

	public static long transfer(InputStream in, WritableByteChannel out,
			MessageDigest digest) throws IOException {
		return transfer(newChannel(in), out, digest);
	}

	public static long transfer(ReadableByteChannel in, OutputStream out,
			MessageDigest digest) throws IOException {
		return transfer(in, newChannel(out), digest);
	}

	public static long transfer(ReadableByteChannel in,
			WritableByteChannel out, MessageDigest digest) throws IOException {
		if (digest == null && in instanceof FileChannel) {
			return ((FileChannel) in).transferTo(0, Long.MAX_VALUE, out);
		} else if (digest == null && out instanceof FileChannel) {
			return ((FileChannel) out).transferFrom(in, 0, Long.MAX_VALUE);
		} else {
			long read = 0;
			ByteBuffer buf = ByteBuffer.allocate(1024 * 8);
			buf.clear();
			while (in.read(buf) >= 0 || buf.position() != 0) {
				buf.flip();
				int len = out.write(buf);
				if (digest != null) {
					digest.update(buf.array(), buf.arrayOffset(), len);
				}
				read += len;
				buf.compact();
			}
			return read;
		}
	}

	public static byte[] newByteArray(InputStream in) throws IOException {
		return newByteArray(newChannel(in));
	}

	public static byte[] newByteArray(ReadableByteChannel ch) throws IOException {
		if (ch == null)
			return null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
			transfer(ch, out);
			return out.toByteArray();
		} finally {
			ch.close();
		}
	}

	public static ReadableByteChannel emptyChannel() {
		return new ReadableByteChannel() {
			private boolean closed;

			public int read(ByteBuffer dst) throws IOException {
				return -1;
			}

			public void close() throws IOException {
				closed = true;
			}

			public boolean isOpen() {
				return !closed;
			}

			@Override
			public String toString() {
				return "empty";
			}
		};
	}

	private static class ChannelInputStream extends FilterInputStream {
		private ReadableByteChannel ch;

		protected ChannelInputStream(ReadableByteChannel ch) {
			super(Channels.newInputStream(ch));
			this.ch = ch;
		}

		public ReadableByteChannel getChannel() {
			return ch;
		}

		@Override
		public String toString() {
			return ch.toString();
		}
	}

	private ChannelUtil() {
	}
}
