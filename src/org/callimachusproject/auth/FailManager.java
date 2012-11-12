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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FailManager {
	private static long resetAttempts;
	private static final Map<Object, Integer> failedAttempts = new HashMap<Object, Integer>();
	private static final int MAX_ENTRIES = 2048;
	private static final Map<Object, Object> replay = new LinkedHashMap<Object, Object>() {
		private static final long serialVersionUID = -6673793531014489904L;

		protected boolean removeEldestEntry(
				Entry<Object, Object> eldest) {
			return size() > MAX_ENTRIES;
		}
	};

	public boolean isReplayed(Object options) {
		synchronized (replay) {
			return replay.put(options, Boolean.TRUE) != null;
		}
	}

	public void failedAttempt(Object user) {
		synchronized (failedAttempts) {
			long now = System.currentTimeMillis();
			if (resetAttempts < now) {
				failedAttempts.clear();
				resetAttempts = now + 60 * 60 * 1000;
			}
			try {
				if (user == null) {
					Thread.sleep(1000);
				} else {
					Integer count = failedAttempts.get(user);
					if (count == null) {
						failedAttempts.put(user, 1);
						Thread.sleep(1000);
					} else if (count > 100) {
						failedAttempts.put(user, count + 1);
						Thread.sleep(10000);
					} else {
						failedAttempts.put(user, count + 1);
						Thread.sleep(1000);
					}
				}
			} catch (InterruptedException e) {
				// continue
			}
		}
	}

}
