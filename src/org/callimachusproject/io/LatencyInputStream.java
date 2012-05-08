package org.callimachusproject.io;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link BufferedInputStream} that does not terminate stream if nothing is
 * available.
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
