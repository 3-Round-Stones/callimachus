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
package org.callimachusproject.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.callimachusproject.Version;

public class SystemProperties {
	private static final String SERVER_DEFAULT_CONF = "etc/callimachus-defaults.conf";
	private static final String REPOSITORY_CONFIG = "etc/callimachus-repository.ttl";
	private static final String VERSION_CODE = Version.getInstance().getVersionCode();
	private static final String WEBAPP_CAR = "lib/callimachus-webapp-" + VERSION_CODE + ".car";

	public static File getMailPropertiesFile() {
		String mail = getProperty("java.mail.properties");
		if (mail == null)
			return new File("etc/mail.properties");
		return new File(mail);
	}

	public static File getRepositoryConfigFile() {
		String rconfig = getProperty("org.callimachusproject.config.repository");
		if (rconfig != null)
			return new File(rconfig);
		return new File(REPOSITORY_CONFIG);
	}

	public static File getWebappCarFile() {
		String car = getProperty("org.callimachusproject.config.webapp");
		if (car != null)
			return new File(car);
		return new File(WEBAPP_CAR);
	}

	public static File getConfigDefaultsFile() {
		String defaultFile = getProperty("org.callimachusproject.config.defaults");
		if (defaultFile != null)
			return new File(defaultFile);
		return new File(SERVER_DEFAULT_CONF);
	}

	public static int getUnlockAfter() {
		String unlockAfter = getProperty("org.callimachusproject.auth.unlockAfter");
		if (unlockAfter != null && Pattern.matches("\\d+", unlockAfter))
			return Math.abs(Integer.parseInt(unlockAfter));
		return 12 * 60 * 60;
	}

	public static int getMaxLoginAttempts() {
		String maxLoginAttempts = getProperty("org.callimachusproject.auth.maxLoginAttempts");
		if (maxLoginAttempts != null && Pattern.matches("\\d+", maxLoginAttempts))
			return Math.abs(Integer.parseInt(maxLoginAttempts));
		return 1000;
	}

	public static Header[] getStaticResponseHeaders() {
		String headers = System.getProperty("org.callimachusproject.auth.headers");
		if (headers == null)
			return null;
		List<Header> list = new ArrayList<Header>();
		for (String header : headers.split(",")) {
			if (header.indexOf(":") > 0) {
				String[] pair = header.split(":", 2);
				list.add(new BasicHeader(pair[0].trim(), pair[1].trim()));
			} else if (!list.isEmpty()) {
				Header last = list.remove(list.size() - 1);
				String value = last.getValue() + "," + header;
				list.add(new BasicHeader(last.getName(), value));
			}
		}
		return list.toArray(new Header[list.size()]);
	}

	private static String getProperty(String key) {
		try {
			return System.getProperty(key);
		} catch (SecurityException e) {
			e.printStackTrace(System.err);
		}
		return null;
	}
}
