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
import java.net.URL;

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
public class SetupProcessor implements DataValidator {
    
    public boolean getDefaultAnswer() {
        return true;
    }
    
    public String getErrorMessageId() {
        return "CallimachusSetupValidator reported an error.  Run this installer from a command line for a full stack trace.";
    }
    
    public String getWarningMessageId() {
        return "CallimachusSetupValidator reported a warning.  Run this installer from a command line for a full stack trace.";
    }
    
    public DataValidator.Status validateData(AutomatedInstallData adata) {

    	String primaryAuthority = adata.getVariable("callimachus.PRIMARY_ORIGIN");
        
		Configure configure = ConfigurationReader.configure;
		
		try {
			if (!configure.isConnected()) {
				URL config = configure.getRepositoryConfigTemplates().values().iterator().next();
				configure.connect(config, null);
				configure.createOrigin(primaryAuthority);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Status.ERROR;
		}
    
        return Status.OK;
    }
    
}