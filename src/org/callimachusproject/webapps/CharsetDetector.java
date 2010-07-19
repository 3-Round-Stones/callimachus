package org.callimachusproject.webapps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.zip.GZIPInputStream;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharsetDetector implements nsICharsetDetectionObserver {
	static final Charset ASCII = Charset.forName("US-ASCII");
	static final Charset DEFAULT = Charset.defaultCharset();
	private Logger logger = LoggerFactory.getLogger(CharsetDetector.class);

	private Charset charset;

	public Charset detect(File file, boolean gzip) throws IOException {
		InputStream in = new FileInputStream(file);
		if (gzip) {
			in = new GZIPInputStream(in);
		}
		try {
			return detect(in);
		} finally {
			in.close();
		}
	}

	public Charset detect(InputStream in) throws IOException {
		boolean ascii = true;
		nsDetector det = new nsDetector();
		det.Init(this);
		int len;
		boolean done = false;
		byte[] buf = new byte[1024];
		while ((len = in.read(buf)) >= 0) {
			// Check if the stream is only ascii.
			if (ascii) {
				ascii = det.isAscii(buf, len);
			}
			// DoIt if non-ascii and not done yet.
			if (!ascii && !done) {
				done = det.DoIt(buf, len, false);
			}
		}
		det.Done();
		if (charset == null && ascii) {
			charset = ASCII;
		} else if (charset == null) {
			for (String name : det.getProbableCharsets()) {
				try {
					if ("nomatch".equals(name))
						continue;
					Charset cs = Charset.forName(name);
					if (charset == null) {
						charset = cs;
					}
					if (DEFAULT.contains(cs))
						return DEFAULT;
					if (cs.contains(DEFAULT))
						return cs;
				} catch (IllegalCharsetNameException e) {
					this.logger.warn(e.toString(), e);
				} catch (UnsupportedCharsetException e) {
					this.logger.warn(e.toString(), e);
				}
			}
		}
		if (charset == null || DEFAULT.contains(charset))
			return DEFAULT;
		return charset;
	}

	public void Notify(String charset) {
		if (charset == null) {
			this.charset = null;
		} else {
			try {
				this.charset = Charset.forName(charset);
			} catch (IllegalCharsetNameException e) {
				this.logger.warn(e.toString(), e);
			} catch (UnsupportedCharsetException e) {
				this.logger.warn(e.toString(), e);
			}
		}
	}
}