/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

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
package org.callimachusproject.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses multipart inputsteams into multiple streamed serial inputstreams.
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
	private StringBuilder headers;

	public MultipartParser(InputStream in) throws IOException {
		this.boundary = readBoundary(in);
		this.in = (in.markSupported() ? in : new BufferedInputStream(in,
				boundary.length + 4));
		this.buffer = new byte[boundary.length];
		this.partEnd = true;
		this.fileEnd = false;
	}

	/**
	 * Aborts reading the stream.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		in.close();
	}

	/**
	 * Call after next() to return the most recent headers parsed from the stream.
	 * 
	 * @return Map with all keys in lowercase and values joined using a comma
	 */
	public Map<String, String> getHeaders() {
		assert headers != null;
		Map<String, String> map = new HashMap<String, String>();
		for (String line : headers.toString().split("\r\n")) {
			String[] hd = line.split(":", 2);
			if (hd.length != 2 || hd[0] == null || hd[0].length() == 0)
				continue;
			String key = hd[0].trim().toLowerCase();
			String value = hd[1].trim();
			if (map.containsKey(key)) {
				map.put(key, map.get(key) + ',' + value);
			} else {
				map.put(key, value);
			}
		}
		return map;
	}

	/**
	 * Parses a new set of headers and returns an InputStream to read the next
	 * part. The previous InputStream must be consumed in full (or closed)
	 * before calling this method. Calling close() on the returned InputStream
	 * will advance the stream to the end of the part.
	 * 
	 * @return an InputStream or null if end of file reached
	 * @throws IOException
	 */
	public InputStream next() throws IOException {
		if (fileEnd)
			return null;
		if (!partEnd)
			throw new IOException("The previous part stream must be closed before the next stream can be read");
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

			public void close() throws IOException {
				while (read() != -1)
					;
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
		headers = new StringBuilder();
		if (read(in, headers) == '\r' && read(in, headers) == '\n')
			return true;
		int ch = read(in, headers);
		while (ch != -1) {
			ch = read(in, headers);
			if (ch == '\r' && read(in, headers) == '\n'
					&& read(in, headers) == '\r' && read(in, headers) == '\n') {
				return true;
			}
		}
		return false;
	}

	private int read(InputStream in, StringBuilder sb) throws IOException {
		int read = in.read();
		if (read != -1) {
			sb.append((char) read);
		}
		return read;
	}
}
