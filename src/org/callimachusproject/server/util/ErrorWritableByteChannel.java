/*
 * Copyright 2010, James Leigh Some rights reserved.
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
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Pipe.SinkChannel;

/**
 * Pipes the data and error messages of an OuputStream as an InputStream.
 * 
 * @author James Leigh
 *
 */
public class ErrorWritableByteChannel implements WritableByteChannel {
	private SinkChannel delegate;
	private IOException e;

	public ErrorWritableByteChannel(Pipe pipe) throws IOException {
		this(pipe.sink());
	}

	public ErrorWritableByteChannel(SinkChannel sink) throws IOException {
		this.delegate = sink;
	}

	public void error(IOException e) {
		this.e = e;
	}

	public boolean isOpen() {
		return delegate.isOpen();
	}

	public int write(ByteBuffer src) throws IOException {
		throwIOException();
		return delegate.write(src);
	}

	public void close() throws IOException {
		throwIOException();
		delegate.close();
	}

	public String toString() {
		return delegate.toString();
	}

	private void throwIOException() throws IOException {
		try {
			if (e != null)
				throw e;
		} finally {
			e = null;
		}
	}
}