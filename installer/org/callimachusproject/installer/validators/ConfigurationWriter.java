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
package org.callimachusproject.installer.validators;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.callimachusproject.installer.Configure;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;

/**
 * A custom IzPack (see http://izpack.org) validator to validate
 * the status of com.izforge.izpack.panels.CallimachusConfigurationPanel.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class ConfigurationWriter implements DataValidator {
	private boolean abort;

	public boolean getDefaultAnswer() {
		return true;
	}

	public String getErrorMessageId() {
		return ConfigurationReader.ERROR_MSG;
	}

	public String getWarningMessageId() {
		return ConfigurationReader.ERROR_MSG;
	}

	public DataValidator.Status validateData(AutomatedInstallData adata) {
		if (abort)
			return Status.ERROR;
		try {

        	Configure configure = (Configure) adata.getAttribute(Configure.class.getName());

			// Write Callimachus callimachus.conf file.
			configure.setServerConfiguration(getServerConfiguration(adata));
			// Write Callimachus mail.properties file.
			configure.setMailProperties(getMailProperties(adata));
			// Write Callimachus logging.properties file
			configure.setLoggingProperties(getLoggingProperties(adata));

			if (configure.isConnected()) {
				String primary = configureOrigins(adata);

				configure.disconnect();
				String startserver = adata.getVariable("callimachus.startserver");
				if ("true".equals(startserver) ) {
					boolean started = configure.startServer();
					String openbrowser = adata.getVariable("callimachus.openbrowser");
					if (started && configure.isWebBrowserSupported() && "true".equals(openbrowser) ) {
						configure.openWebBrowser(primary + "/");
					}
				}
			}

			return Status.OK;
		} catch (Exception e) {
			abort = true;
			// This is an unknown error.
			e.printStackTrace();
			return Status.ERROR;
		}
	}

	public Properties getServerConfiguration(AutomatedInstallData adata) throws IOException {
    	Configure configure = (Configure) adata.getAttribute(Configure.class.getName());
		String primary = getSingleLine(adata, "callimachus.PRIMARY_ORIGIN");
		String secondary = getSingleLine(adata, "callimachus.SECONDARY_ORIGIN");
		String other = getSingleLine(adata, "callimachus.OTHER_REALM");
		Properties conf = configure.getServerConfiguration();
		conf.setProperty("PORT", getSingleLine(adata, "callimachus.PORT"));
		conf.setProperty("PRIMARY_ORIGIN", primary);
		conf.setProperty("SECONDARY_ORIGIN", secondary);
		conf.setProperty("OTHER_REALM", other);
		conf.setProperty("ALL_LOCAL", getSingleLine(adata, "callimachus.ALL_LOCAL"));
		// Set the origin on disk to be the space-separated concatenation of origins
		StringBuilder origin = new StringBuilder();
		origin.append(primary).append(" ");
		origin.append(secondary).append(" ");
		origin.append(other.replaceAll("(://[^/]*)/\\S*", "$1")).append(" ");
		conf.setProperty("ORIGIN", origin.toString().trim());
		// save JAVA_HOME
		String jdk = adata.getVariable("JDKPath");
		if (conf.getProperty("JAVA_HOME") == null && jdk != null) {
			File jre = new File(jdk, "jre");
			File bin = new File(jre, "bin");
			File java = new File(bin, "java");
			File java_exe = new File(bin, "java.exe");
			if (java.isFile() || java_exe.isFile()) {
				conf.setProperty("JAVA_HOME", jre.getAbsolutePath());
			}
		}
		return conf;
	}

	public Properties getMailProperties(AutomatedInstallData adata) throws IOException {
    	Configure configure = (Configure) adata.getAttribute(Configure.class.getName());
		Properties mail = configure.getMailProperties();
		if ("true".equals(adata.getVariable("callimachus.later.mail"))) {
			mail.remove("mail.transport.protocol");
		} else {
			mail.setProperty("mail.transport.protocol", getSingleLine(adata, "callimachus.mail.transport.protocol") );
		}
		mail.setProperty("mail.from", getSingleLine(adata, "callimachus.mail.from") );
		mail.setProperty("mail.smtps.host", getSingleLine(adata, "callimachus.mail.smtps.host") );
		mail.setProperty("mail.smtps.port", getSingleLine(adata, "callimachus.mail.smtps.port") );
		mail.setProperty("mail.smtps.auth", getSingleLine(adata, "callimachus.mail.smtps.auth") );
		mail.setProperty("mail.user", getSingleLine(adata, "callimachus.mail.user") );
		mail.setProperty("mail.password", getSingleLine(adata, "callimachus.mail.password") );
		return mail;
	}

	public Properties getLoggingProperties(AutomatedInstallData adata)
			throws IOException {
    	Configure configure = (Configure) adata.getAttribute(Configure.class.getName());
		return configure.getLoggingProperties();
	}

	private String getSingleLine(AutomatedInstallData adata, String key) {
		String value = adata.getVariable(key);
		if (value == null)
			return "";
		return value.trim().replaceAll("\\s+", " ");
	}

	private String configureOrigins(AutomatedInstallData adata) throws Exception {
    	Configure configure = (Configure) adata.getAttribute(Configure.class.getName());
		String primary = adata.getVariable("callimachus.PRIMARY_ORIGIN").split("\\s+")[0];
		String secondary = adata.getVariable("callimachus.SECONDARY_ORIGIN");
		for (String origin : secondary.split("\\s+")) {
			if (origin.length() > 0) {
				configure.createVirtualHost(origin, primary);
			}
		}
		String other = adata.getVariable("callimachus.OTHER_REALM");
		for (String realm : other.split("\\s+")) {
			if (realm.length() > 0) {
				configure.createRealm(realm, primary);
			}
		}
		if ("true".equals(adata.getVariable("callimachus.ALL_LOCAL"))) {
			configure.setResourcesAsLocalTo(primary);
		} else {
			configure.setResourcesAsLocalTo(null);
		}
		return primary;
	}

}