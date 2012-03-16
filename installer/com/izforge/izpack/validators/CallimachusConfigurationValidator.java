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
package com.izforge.izpack.validators;
import com.izforge.izpack.installer.DataValidator;
import com.izforge.izpack.installer.DataValidator.Status;
import com.izforge.izpack.installer.AutomatedInstallData;

/**
 * A custom IzPack (see http://izpack.org) validator to validate
 * the status of com.izforge.izpack.panels.CallimachusConfigurationPanel.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class CallimachusConfigurationValidator implements DataValidator {
    
    static String DATA_VALIDATOR_CLASSNAME_TAG = "CallimachusConfigurationValidator";
    static String DATA_VALIDATOR_TAG = "CallimachusConfigurationValidator tag";
    
    public boolean getDefaultAnswer() {
        return true;
    }
    
    public String getErrorMessageId() {
        return "CallimachusConfigurationPanel reported an error.  Run this installer from a command line for a full stack trace.";
    }
    
    public String getWarningMessageId() {
        return "CallimachusConfigurationPanel reported a warning.  Run this installer from a command line for a full stack trace.";
    }
    
    public DataValidator.Status validateData(AutomatedInstallData adata) {
        String exitStatus = adata.getVariable("callimachus.CallimachusConfigurationValidator.exitStatus");
        
        // TODO: Remove
        System.err.println("In validateData(): exitStatus = " + exitStatus);
        
        if ( exitStatus.equals("error") ) {
            return Status.ERROR;
        } else if ( exitStatus.equals("warning") ) {
            return Status.WARNING;
        }
        
        // TODO: Remove
        System.err.println("  Returning OK.");
        
        return Status.OK;
    }
}