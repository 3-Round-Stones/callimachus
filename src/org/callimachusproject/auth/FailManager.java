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
package org.callimachusproject.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.callimachusproject.util.SystemProperties;

public class FailManager {
	private static long resetAttempts;
	private static final ConcurrentMap<String, Integer> failedAttempts = new ConcurrentHashMap<String, Integer>();
	private static final int MAX_ENTRIES = 2048;
	private static final Map<Object, Object> replay = new LinkedHashMap<Object, Object>() {
		private static final long serialVersionUID = -6673793531014489904L;

		protected boolean removeEldestEntry(
				Entry<Object, Object> eldest) {
			return size() > MAX_ENTRIES;
		}
	};
	private int RESET_ATTEMPTS = SystemProperties.getUnlockAfter() * 1000;
	private int MAX_LOGIN_ATTEMPTS = SystemProperties.getMaxLoginAttempts();
	private int THROTTLE_ATTEMPTS = (int) Math.ceil(MAX_LOGIN_ATTEMPTS / 10.0);

	public boolean isReplayed(Object options) {
		synchronized (replay) {
			return replay.put(options, Boolean.TRUE) != null;
		}
	}

	public int retryAfter(String username) {
		int failures = getFailures(username);
		if (failures > MAX_LOGIN_ATTEMPTS) {
			long now = System.currentTimeMillis();
			return (int) Math.max(0, resetAttempts - now) / 1000 + 1;
		}
		return 0;
	}

	public void successfulAttempt(String username) {
		int failures = getFailures(username);
		if (failures > 0) {
			penalize(failures);
			failedAttempts.remove(username, failures);
		}
	}

	public void failedAttempt(String username) {
		penalize(incrementFailures(username));
	}

	private int getFailures(String username) {
		if (username == null || !failedAttempts.containsKey(username)) {
			return 0;
		} else {
			synchronized (failedAttempts) {
				long now = System.currentTimeMillis();
				if (resetAttempts < now) {
					failedAttempts.clear();
				}
				Integer count = failedAttempts.get(username);
				if (count == null) {
					return 0;
				} else {
					return count;
				}
			}
		}
	}

	private int incrementFailures(String username) {
		synchronized (failedAttempts) {
			long now = System.currentTimeMillis();
			if (resetAttempts < now) {
				failedAttempts.clear();
				resetAttempts = now + RESET_ATTEMPTS;
			}
			if (username == null) {
				return 1;
			} else {
				Integer count = failedAttempts.get(username);
				if (count == null) {
					failedAttempts.put(username, 1);
					return 1;
				} else if (count < Integer.MAX_VALUE) {
					failedAttempts.put(username, count + 1);
					return count + 1;
				} else {
					return count;
				}
			}
		}
	}

	private void penalize(int count) {
		synchronized (failedAttempts) {
			try {
				if (count > THROTTLE_ATTEMPTS) {
					Thread.sleep(10000);
				} else {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				// continue
			}
		}
	}

}
