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

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link BufferedInputStream} that does not terminate stream if nothing is
 * available. If {@link #read(byte[], int, int)} or {@link #skip(long)} return
 * non-positive number then EOF has been reached (unless used with a non-positive parameter).
 * 
 * @author James Leigh
 * 
 */
public class LatencyInputStream extends BufferedInputStream {

	public LatencyInputStream(InputStream in) {
		this(in, 65536);
	}

	public LatencyInputStream(InputStream in, int size) {
		super(new FilterInputStream(in) {
			public int read(byte[] b, int off, int len) throws IOException {
				int read;
				while ((read = super.read(b, off, len)) == 0) {
					Thread.yield();
					if (Thread.interrupted()) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				return read;
			}
		}, size);
	}

}
