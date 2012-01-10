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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.entity.ProducingNHttpEntity;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.ReadableContentListener;

/**
 * Redirects read operations to a {@link ReadableContentListener} objects.
 * 
 * @author James Leigh
 * 
 */
public class ConsumingHttpEntity extends ConsumingNHttpEntityTemplate implements
		ProducingNHttpEntity {
	private ReadableContentListener in;
	private ByteBuffer buf;

	public ConsumingHttpEntity(HttpEntity httpEntity, ReadableContentListener in) {
		super(httpEntity, in);
		this.in = in;
	}

	@Override
	public String toString() {
		return in.toString();
	}

	@Override
	public InputStream getContent() throws IOException,
			UnsupportedOperationException {
		return ChannelUtil.newInputStream(in);
	}

	@Override
	public void writeTo(OutputStream out) throws IOException,
			UnsupportedOperationException {
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
	public boolean isStreaming() {
		return super.isStreaming() && in.isOpen();
	}

	public ReadableByteChannel getReadableByteChannel() throws IOException {
		return in;
	}

	public void produceContent(ContentEncoder encoder, IOControl ioctrl)
			throws IOException {
		if (buf == null) {
			buf = ByteBuffer.allocate(1024);
		}
		if (in.read(buf) < 0 && buf.position() == 0) {
			encoder.complete();
		} else {
			buf.flip();
			encoder.write(buf);
			buf.compact();
		}

	}

}
