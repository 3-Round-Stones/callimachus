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
import java.net.SocketException;
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

			setupSecondaryOrigin(adata);
			
			if (!configure.isConnected()) {
				URL config = configure.getRepositoryConfigTemplates().get("Sesame");
				assert config != null;
				configure.connect(config, null);
		    	String primary = adata.getVariable("callimachus.PRIMARY_ORIGIN");
				for (String origin : primary.split("\\s+")) {
					configure.createOrigin(origin);
				}
			}
		    
	        return Status.OK;
		} catch (Exception e) {
			abort = true;
			e.printStackTrace();
			return Status.ERROR;
		}
    }

	private void setupSecondaryOrigin(AutomatedInstallData adata) throws SocketException {
    	Configure configure = (Configure) adata.getAttribute(Configure.class.getName());
    	String primary = adata.getVariable("callimachus.PRIMARY_ORIGIN");
		if ("".equals(adata.getVariable("callimachus.SECONDARY_ORIGIN"))) {
			StringBuilder sb = new StringBuilder();
			String port = adata.getVariable("callimachus.PORT");
			for (String origin : configure.getDefaultOrigins(port)) {
				if (!primary.contains(origin)) {
					sb.append(origin).append("\n");
				}
			}
			adata.setVariable("callimachus.SECONDARY_ORIGIN", sb.toString());
		}
	}
    
}