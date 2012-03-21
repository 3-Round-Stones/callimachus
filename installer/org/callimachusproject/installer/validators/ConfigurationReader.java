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
import java.net.SocketException;
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
public class ConfigurationReader implements DataValidator {
	public static final String ERROR_MSG = "There was an error during the install. You must abort this instalation and try again. Run this installer from a command line for a full stack trace.";
	private boolean abort;
    
    public boolean getDefaultAnswer() {
        return true;
    }
    
    public String getErrorMessageId() {
        return ERROR_MSG;
    }
    
    public String getWarningMessageId() {
        return ERROR_MSG;
    }
    
    public DataValidator.Status validateData(AutomatedInstallData adata) {
    	if (abort)
    		return Status.ERROR;
        try {
        	Configure configure = (Configure) adata.getAttribute(Configure.class.getName());
			if (configure == null) {
				String installPath = adata.getInstallPath();
				configure = new Configure(new File(installPath));
				adata.setAttribute(Configure.class.getName(), configure);
				if (configure.isServerRunning() && !configure.stopServer()) {
					System.err.println("Server must be shut down to continue");
	    			return Status.ERROR;
	    		}
			}

			// Set IzPack variables for callimachus.conf:
			String[] confProperties = {"PORT", "PRIMARY_ORIGIN", "SECONDARY_ORIGIN", "OTHER_REALM", "ALL_LOCAL", "DAEMON_USER", "DAEMON_GROUP"};
			Properties conf = configure.getServerConfiguration();
			if (conf.getProperty("PRIMARY_ORIGIN") == null && conf.getProperty("ORIGIN") == null) {
				// new install
				conf.setProperty("PRIMARY_ORIGIN", getDefaultPrimaryOrigin(configure));
				adata.setVariable("callimachus.upgrading", "false");
			} else if (conf.getProperty("PRIMARY_ORIGIN") == null) {
				// upgrading from 0.15
				conf.setProperty("PRIMARY_ORIGIN", conf.getProperty("ORIGIN"));
				adata.setVariable("callimachus.upgrading", "true");
			} else {
				adata.setVariable("callimachus.upgrading", "true");
			}
			setCallimachusVariables(conf, confProperties, adata);

			// Set IzPack variables for mail.properties:
			String[] mailProperties = {"mail.transport.protocol", "mail.from", "mail.smtps.host", "mail.smtps.port", "mail.smtps.auth", "mail.user", "mail.password"};
			setCallimachusVariables(configure.getMailProperties(), mailProperties, adata);
		} catch (Exception e) {
			abort = true;
			// This is an unknown error.
    		e.printStackTrace();
			return Status.ERROR;
		}
        
        return Status.OK;
    }

	private String getDefaultPrimaryOrigin(Configure configure)
			throws SocketException {
		for (String origin : configure.getDefaultOrigins("80")) {
			if (origin.matches("\\.([A-Z]{2}|com|org|net|biz|info|name|aero|biz|info|jobs|museum|name)$")) {
				if ("root".equals(System.getProperty("user.name")))
					return origin;
				return origin + ":8080";
			}
		}
		return "http://localhost:8080";
	}
    
    /**
	 * Set IzPack variables for each property provided.
	 *
	 * @param prop A Java Properties object holding values from a Callimachus config file.
	 * @param names A list of property names to convert to IzPack variables.
	 */
	private void setCallimachusVariables(Properties prop, String[] names, AutomatedInstallData adata) {
		// Get the values of relevant properties and convert them to IzPack
		// variables with the same names but prepended by "callimachus." to
		// avoid namespace conflicts.
		int propertiesLength = names.length;
		for (int i = 0; i < propertiesLength; i++) {
			String value = prop.getProperty(names[i]);
			String key = "callimachus." + names[i];
			if (value == null) {
				adata.setVariable(key, "");
			} else {
				adata.setVariable(key, value.replaceAll("\\s+", "\n"));
			}
		}
	}
}