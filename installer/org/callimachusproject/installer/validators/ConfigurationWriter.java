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
		try {

	        Configure configure = ConfigurationReader.configure;
	        
            saveConfig(adata, configure);
            saveMail(adata, configure);
    		saveLogging(configure);

			if (configure.isConnected()) {
		    	String primary = configureOrigins(adata, configure);

				configure.disconnect();
				if ("true".equals(getSingleLine(adata, "callimachus.startserver")) ) {
					boolean started = configure.startServer();
					if (started && configure.isWebBrowserSupported() && "true".equals(getSingleLine(adata, "callimachus.openbrowser")) ) {
						configure.openWebBrowser(primary + "/");
					}
				}
			}

	        return Status.OK;
		} catch (Exception e) {
			// This is an unknown error.
    		e.printStackTrace();
			return Status.ERROR;
		}
    }

	private String configureOrigins(AutomatedInstallData adata,
			Configure configure) throws Exception {
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
			configure.mapAllResourcesAsLocal(primary);
		}
		return primary;
	}

	private void saveConfig(AutomatedInstallData adata, Configure configure)
			throws IOException {
		// Write Callimachus callimachus.conf file.
		String primary = getSingleLine(adata, "callimachus.PRIMARY_ORIGIN");
		String secondary = getSingleLine(adata, "callimachus.SECONDARY_ORIGIN");
		String other = getSingleLine(adata, "callimachus.OTHER_REALM");
		Properties confProperties = configure.getServerConfiguration();
		confProperties.setProperty("PORT", getSingleLine(adata, "callimachus.PORT"));
		confProperties.setProperty("PRIMARY_ORIGIN", primary);
		confProperties.setProperty("SECONDARY_ORIGIN", secondary);
		confProperties.setProperty("OTHER_REALM", other);
		confProperties.setProperty("ALL_LOCAL", getSingleLine(adata, "callimachus.ALL_LOCAL"));
		// Set the origin on disk to be the space-separated concatenation of origins
		StringBuilder origin = new StringBuilder();
		origin.append(primary).append(" ");
		origin.append(secondary).append(" ");
		origin.append(other.replaceAll("(://[^/]*)/\\S*", "$1")).append(" ");
		confProperties.setProperty("ORIGIN", origin.toString().trim());
		configure.setServerConfiguration(confProperties);
	}

	private void saveMail(AutomatedInstallData adata, Configure configure)
			throws IOException {
		// Write Callimachus mail.properties file.
		Properties mailProperties = configure.getMailProperties();
		// NB: Ensure that these var names are correct in install.xml, userInputSpec.xml
		mailProperties.setProperty("mail.transport.protocol", getSingleLine(adata, "callimachus.mail.transport.protocol") );
		mailProperties.setProperty("mail.from", getSingleLine(adata, "callimachus.mail.from") );
		mailProperties.setProperty("mail.smtps.host", getSingleLine(adata, "callimachus.mail.smtps.host") );
		mailProperties.setProperty("mail.smtps.port", getSingleLine(adata, "callimachus.mail.smtps.port") );
		mailProperties.setProperty("mail.smtps.auth", getSingleLine(adata, "callimachus.mail.smtps.auth") );
		mailProperties.setProperty("mail.user", getSingleLine(adata, "callimachus.mail.user") );
		mailProperties.setProperty("mail.password", getSingleLine(adata, "callimachus.mail.password") );
		configure.setMailProperties(mailProperties);
	}

	private void saveLogging(Configure configure) throws IOException {
		// Write Callimachus logging.properties file
		configure.setLoggingProperties(configure.getLoggingProperties());
	}

	private String getSingleLine(AutomatedInstallData adata, String key) {
		String value = adata.getVariable(key);
		if (value == null)
			return "";
		return value.trim().replaceAll("\\s+", " ");
	}
       
}