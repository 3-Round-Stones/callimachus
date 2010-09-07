/*
   Copyright 2009 Zepheira LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.helpers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parses multipart inputsteams into multiple inputstream.
 * 
 * @author James Leigh
 *
 */
public class MultipartParser {
	private byte[] boundary = null;
	private byte[] buffer = null;
	private boolean fileEnd = false;
	private InputStream in = null;
	private boolean partEnd = false;

	public MultipartParser(InputStream in) throws IOException {
		this.boundary = readBoundary(in);
		this.in = (in.markSupported() ? in : new BufferedInputStream(in,
				boundary.length + 4));
		this.buffer = new byte[boundary.length];
		this.partEnd = true;
		this.fileEnd = false;
	}

	public void close() throws IOException {
		in.close();
	}

	public InputStream next() throws IOException {
		if (fileEnd)
			return null;
		assert partEnd;
		partEnd = false;
		if (!readHeaders())
			return null;
		return new InputStream() {
			public int read() throws IOException {
				int ch;
				if (partEnd)
					return -1;
				switch (ch = in.read()) {
				case '\r':
					in.mark(boundary.length + 3);
					int c1 = in.read();
					int c2 = in.read();
					int c3 = in.read();
					if ((c1 == '\n') && (c2 == '-') && (c3 == '-')) {
						if (!readBoundaryBytes()) {
							in.reset();
							return ch;
						}
						for (int i = 0; i < boundary.length; i++) {
							if (buffer[i] != boundary[i]) {
								in.reset();
								return ch;
							}
						}
						partEnd = true;
						ch = in.read();
						if (ch == '\r') {
							ch = in.read();
							assert ch == '\n';
						} else if (ch == '-') {
							if (in.read() == '-')
								fileEnd = true;
						} else {
							fileEnd = (ch == -1);
						}
						return -1;
					} else {
						in.reset();
						return ch;
					}
				case -1:
					fileEnd = true;
					return -1;
				default:
					return ch;
				}
			}

			private final boolean readBoundaryBytes() throws IOException {
				int pos = 0;
				while (pos < buffer.length) {
					int got = in.read(buffer, pos, buffer.length - pos);
					if (got < 0)
						return false;
					pos += got;
				}
				return true;
			}
		};
	}

	private byte[] readBoundary(InputStream in) throws IOException {
		int ch;
		ch = in.read();
		assert ch == '-';
		ch = in.read();
		assert ch == '-';
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while ((ch = in.read()) != '\r' && ch != -1) {
			baos.write(ch);
		}
		assert ch != -1;
		ch = in.read();
		assert ch == '\n';
		return baos.toByteArray();
	}

	private boolean readHeaders() throws IOException {
		if (in.read() == '\r' && in.read() == '\n')
			return true;
		int ch = in.read();
		while (ch != -1) {
			ch = in.read();
			if (ch == '\r' && in.read() == '\n' && in.read() == '\r'
					&& in.read() == '\n') {
				return true;
			}
		}
		return false;
	}
}