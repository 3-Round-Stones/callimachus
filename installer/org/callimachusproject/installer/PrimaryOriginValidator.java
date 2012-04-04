/*
 * Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.installer;

import java.net.URI;
import java.net.URISyntaxException;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;

public class PrimaryOriginValidator implements DataValidator {
	private String warning;
	private boolean abort;

	public boolean getDefaultAnswer() {
		return false;
	}

	public String getErrorMessageId() {
		return ConfigurationReader.ERROR_MSG;
	}

	public String getWarningMessageId() {
		return warning;
	}

	public DataValidator.Status validateData(AutomatedInstallData adata) {
		if (abort)
			return Status.ERROR;
		try {
			String primary = adata.getVariable("callimachus.PRIMARY_ORIGIN");
			adata.setVariable("callimachus.PORT", getDefaultPort(primary, false));
			adata.setVariable("callimachus.SSLPORT", getDefaultPort(primary, true));

			String port = getDefaultPort(primary, null);
			if (!new PortAvailabilityValidator().validate(port)) {
				warning = "Port " + port + " is currently in use by another process";
				return Status.WARNING;
			}

			return Status.OK;
		} catch (Exception e) {
			abort = true;
			e.printStackTrace();
			return Status.ERROR;
		}
	}

	private String getDefaultPort(String origins, Boolean ssl) throws URISyntaxException {
		StringBuilder sb = new StringBuilder();
		for (String origin : origins.split("\\s+")) {
			URI uri = new URI(origin + "/");
			if (ssl != null && ssl && !"https".equalsIgnoreCase(uri.getScheme()))
				continue;
			if (ssl != null && !ssl && "https".equalsIgnoreCase(uri.getScheme()))
				continue;
			int port = uri.getPort();
			if (port < 0 && "http".equalsIgnoreCase(uri.getScheme())) {
				port = 80;
			} else if (port < 0 && "https".equalsIgnoreCase(uri.getScheme())) {
				port = 443;
			}
			if (port > 0) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(port);
			}
		}
		if (sb.length() == 0)
			return "8080";
		return sb.toString();
	}

}