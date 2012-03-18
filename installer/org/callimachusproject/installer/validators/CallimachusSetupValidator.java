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
import com.izforge.izpack.installer.AutomatedInstallData;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.callimachusproject.installer.Configure;

/**
 * A custom IzPack (see http://izpack.org) validator to validate
 * the status of com.izforge.izpack.panels.CallimachusConfigurationPanel.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class CallimachusSetupValidator implements DataValidator {
    
    static String DATA_VALIDATOR_CLASSNAME_TAG = "CallimachusSetupValidator";
    static String DATA_VALIDATOR_TAG = "CallimachusSetupValidator tag";
	protected AutomatedInstallData adata;
	protected Map<String,String> defaults = new HashMap<String,String>(20); // Default values for variables.
    
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

        this.adata = adata;
    	String primaryAuthority = adata.getVariable("callimachus.ORIGIN");
        
		String installPath = adata.getInstallPath();
		Configure configure = new Configure(new File(installPath));
		
		try {
    		boolean running = configure.isServerRunning();
			configure.setLoggingProperties(configure.getLoggingProperties());
			if (running) {
    			if (!configure.stopServer()) {
    				System.err.println("Server must be shut down to continue");
        			return Status.ERROR;
    			}
    		}
			URL config = configure.getRepositoryConfigTemplates().values().iterator().next();
			configure.connect(config, null);
			configure.createOrigin(primaryAuthority);
		} catch (Exception e) {
			e.printStackTrace();
			return Status.ERROR;
		}
    
        return Status.OK;
    }
    
}