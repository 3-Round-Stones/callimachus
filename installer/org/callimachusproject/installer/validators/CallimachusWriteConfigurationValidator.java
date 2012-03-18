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
import com.izforge.izpack.installer.DataValidator;
import com.izforge.izpack.installer.DataValidator.Status;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.callimachusproject.installer.Configure;
import com.izforge.izpack.installer.AutomatedInstallData;

/**
 * A custom IzPack (see http://izpack.org) validator to validate
 * the status of com.izforge.izpack.panels.CallimachusConfigurationPanel.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class CallimachusWriteConfigurationValidator implements DataValidator {
    
    static String DATA_VALIDATOR_CLASSNAME_TAG = "CallimachusWriteConfigurationValidator";
    static String DATA_VALIDATOR_TAG = "CallimachusWriteConfigurationValidator tag";
	protected AutomatedInstallData adata;
    
    public boolean getDefaultAnswer() {
        return true;
    }
    
    public String getErrorMessageId() {
        return "CallimachusWriteConfigurationValidator reported an error.  Run this installer from a command line for a full stack trace.";
    }
    
    public String getWarningMessageId() {
        return "CallimachusWriteConfigurationValidator reported a warning.  Run this installer from a command line for a full stack trace.";
    }
    
    public DataValidator.Status validateData(AutomatedInstallData adata) {

        this.adata = adata;
        
        String installPath = adata.getInstallPath();
        Configure configure = new Configure(new File(installPath));
        
    	String primaryAuthority = adata.getVariable("callimachus.ORIGIN");
		try {
        	// Write Callimachus configuration file.
        	Properties confProperties = configure.getServerConfiguration();
            // NB: Ensure that these var names are correct in install.xml, userInputSpec.xml
            confProperties.setProperty("PORT", adata.getVariable("callimachus.PORT") );
            confProperties.setProperty("acceptallrealms", adata.getVariable("callimachus.acceptallrealms") );
            confProperties.setProperty("describeall", adata.getVariable("callimachus.describeall") );
            confProperties.setProperty("otherrealm1", adata.getVariable("callimachus.otherrealm1") );
            confProperties.setProperty("otherrealm2", adata.getVariable("callimachus.otherrealm2") );
            confProperties.setProperty("otherrealm3", adata.getVariable("callimachus.otherrealm3") );
            confProperties.setProperty("otherrealm4", adata.getVariable("callimachus.otherrealm4") );
            confProperties.setProperty("otherrealm5", adata.getVariable("callimachus.otherrealm5") );
            confProperties.setProperty("startserver", adata.getVariable("callimachus.startserver") );
            confProperties.setProperty("openbrowser", adata.getVariable("callimachus.openbrowser") );
            // Set the origin on disk to be the space-separated concatenation of the
            // primary and secondary authorities.
            String origin = "";
            String[] authorities = {"ORIGIN", "secondaryauthority1", "secondaryauthority2", "secondaryauthority3", "secondaryauthority4", "secondaryauthority5"};
            for (int i = 0; i < authorities.length; i++) {
                if ( origin.length() > 0 ) { origin += " "; }
                origin += adata.getVariable("callimachus." + authorities[i]);
            }
            confProperties.setProperty("ORIGIN", origin );
    		configure.setServerConfiguration(confProperties);
        } catch (IOException e) {
            // This is an unknown error.
        	e.printStackTrace();
			return Status.ERROR;
        }
        
    	try {
            // Write Callimachus mail properties file.\
        	Properties mailProperties = configure.getMailProperties();
            // NB: Ensure that these var names are correct in install.xml, userInputSpec.xml
            mailProperties.setProperty("mail.transport.protocol", adata.getVariable("callimachus.mail.transport.protocol") );
            mailProperties.setProperty("mail.from", adata.getVariable("callimachus.mail.from") );
            mailProperties.setProperty("mail.smtps.host", adata.getVariable("callimachus.mail.smtps.host") );
            mailProperties.setProperty("mail.smtps.port", adata.getVariable("callimachus.mail.smtps.port") );
            mailProperties.setProperty("mail.smtps.auth", adata.getVariable("callimachus.mail.smtps.auth") );
            mailProperties.setProperty("mail.user", adata.getVariable("callimachus.mail.user") );
            mailProperties.setProperty("mail.password", adata.getVariable("callimachus.mail.password") );
    		configure.setMailProperties(mailProperties);
        } catch (IOException e) {
            // This is an unknown error.
            e.printStackTrace();
			return Status.ERROR;
        }

    	try {
    		boolean running = configure.isServerRunning();
			configure.setLoggingProperties(configure.getLoggingProperties());
			configure.disconnect();
			if (running && (adata.getVariable("callimachus.startserver")).equals("on") ) {
				boolean started = configure.startServer();
				if (started && configure.isWebBrowserSupported() && (adata.getVariable("callimachus.openbrowser")).equals("on") ) {
					configure.openWebBrowser(primaryAuthority + "/");
				}
			}
		} catch (Exception e) {
			// This is an unknown error.
    		e.printStackTrace();
			return Status.ERROR;
		}
        
        return Status.OK;
    }
       
}