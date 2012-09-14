package org.callimachusproject.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class TextReader extends Reader {
	private final InputStreamReader delegate;

	public TextReader(InputStream in) throws IOException {
		this(in, Charset.defaultCharset());
	}

	public TextReader(InputStream in, String charset) throws IOException {
		this(in, Charset.forName(charset));
	}

	public TextReader(InputStream in, Charset defaultCharset) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(64);
		BufferedInputStream buffer = new BufferedInputStream(in, buf.limit());
		buffer.mark(buf.limit());
		while (buf.hasRemaining()) {
			int read = buffer.read(buf.array(), buf.position(),
					buf.remaining());
			if (read < 0)
				break;
			buf.position(buf.position() + read);
		}
		buffer.reset();
		ByteArrayInputStream peek = new ByteArrayInputStream(buf.array(), 0, buf.position());
		Charset charset = new CharsetDetector(defaultCharset).detect(peek);
		delegate = new InputStreamReader(buffer, charset);
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		return delegate.read(cbuf, off, len);
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

}
