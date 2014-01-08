package org.callimachusproject.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.BitSet;

public class PercentCodec {
	public static final BitSet ALLOWED = new BitSet(256);
	static {
		// alpha characters
		for (int i = 'a'; i <= 'z'; i++) {
			ALLOWED.set(i);
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			ALLOWED.set(i);
		}
		// numeric characters
		for (int i = '0'; i <= '9'; i++) {
			ALLOWED.set(i);
		}
		for (char c : new char[] {/* gen-delims */':', '/', '?', '#', '[', ']',
				'@',/* sub-delims */'!', '$', '&', '\'', '(', ')', '*', '+',
				',', ';', '=',/* unreserved */'-', '.', '_', '~', '%' }) {
			ALLOWED.set(c);
		}
	}

	public static String encode(String decoded) {
		try {
			// JavaScript's decodeURIComponent does not decode '+' as space
			return URLEncoder.encode(decoded, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	public static String decode(String encoded) {
		try {
			return URLDecoder.decode(encoded, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	public static String encodeOthers(String sourceValue, BitSet allowed)
			throws UnsupportedEncodingException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] source = sourceValue.getBytes(Charset.forName("UTF-8"));
		for (byte c : source) {
			if (allowed.get((char) c)) {
				out.write(c);
			} else {
				out.write('%');
				char high = Character.forDigit((c >> 4) & 0xF, 16);
				char low = Character.forDigit(c & 0xF, 16);
				out.write(Character.toUpperCase(high));
				out.write(Character.toUpperCase(low));
			}
		}
		return new String(out.toByteArray(), "UTF-8");
	}
}
