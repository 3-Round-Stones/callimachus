package org.callimachusproject.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class PercentCodec {

	public static String encode(String username) {
		try {
			// JavaScript's decodeURIComponent does not decode '+' as space
			return URLEncoder.encode(username, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	public static String decode(String username) {
		try {
			return URLDecoder.decode(username, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
}
