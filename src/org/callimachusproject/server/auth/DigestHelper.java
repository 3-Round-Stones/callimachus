/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
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
package org.callimachusproject.server.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public class DigestHelper {
	private static final Pattern TOKENS_REGEX = Pattern
			.compile("\\s*([\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\.\\^\\_\\`\\~]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|([^,\"]*)))?\\s*,?");
	private static long resetAttempts;
	private static final Map<String, Integer> failedAttempts = new HashMap<String, Integer>();
	private static final int MAX_ENTRIES = 2048;
	private static final Map<Map<String, String>, Object> replay = new LinkedHashMap<Map<String, String>, Object>() {
		private static final long serialVersionUID = -6673793531014489904L;

		protected boolean removeEldestEntry(
				Entry<Map<String, String>, Object> eldest) {
			return size() > MAX_ENTRIES;
		}
	};
	private static final Map<String, String> DIGEST_OPTS = new HashMap<String, String>();
	static {
		DIGEST_OPTS.put("realm", null);
		DIGEST_OPTS.put("nonce", null);
		DIGEST_OPTS.put("username", null);
		DIGEST_OPTS.put("uri", null);
		DIGEST_OPTS.put("qop", null);
		DIGEST_OPTS.put("cnonce", null);
		DIGEST_OPTS.put("nc", null);
		DIGEST_OPTS.put("response", null);
	}

	public Map<String, String> parseDigestAuthorization(
			Collection<String> authorization) {
		for (String digest : authorization) {
			if (digest == null || !digest.startsWith("Digest "))
				continue;
			String options = digest.substring("Digest ".length());
			Map<String, String> result = new HashMap<String, String>(
					DIGEST_OPTS);
			Matcher m = TOKENS_REGEX.matcher(options);
			while (m.find()) {
				String key = m.group(1);
				if (result.containsKey(key)) {
					if (m.group(2) != null) {
						result.put(key, m.group(2));
					} else if (m.group(3) != null) {
						result.put(key, m.group(3));
					}
				}
			}
			return result;
		}
		return null;
	}

	public boolean isReplayed(Map<String, String> options) {
		boolean replayed;
		synchronized (replay) {
			replayed = replay.put(options, Boolean.TRUE) != null;
		}
		if (replayed) {
			failedAttempt(options.get("username"));
		}
		return replayed;
	}

	public void failedAttempt(String username) {
		synchronized (failedAttempts) {
			long now = System.currentTimeMillis();
			if (resetAttempts < now) {
				failedAttempts.clear();
				resetAttempts = now + 60 * 60 * 1000;
			}
			try {
				if (username == null || "logout".equals(username)) {
					Thread.sleep(1000);
				} else {
					Integer count = failedAttempts.get(username);
					if (count == null) {
						failedAttempts.put(username, 1);
						Thread.sleep(1000);
					} else if (count > 100) {
						failedAttempts.put(username, count + 1);
						Thread.sleep(10000);
					} else {
						failedAttempts.put(username, count + 1);
						Thread.sleep(1000);
					}
				}
			} catch (InterruptedException e) {
				// continue
			}
		}
	}

	public String md5(String text) {
		return new String(Hex.encodeHex(DigestUtils.md5(text)));
	}

	public String hash(String... values) {
		long code = 0;
		if (values != null) {
			for (String str : values) {
				code = code * 31 + str.hashCode();
			}
		}
		return Long.toString(code, Character.MAX_RADIX);
	}

	public String encode(String username) {
		try {
			return URLEncoder.encode(username, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

}
