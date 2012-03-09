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
package com.izforge.izpack.panels;
import com.izforge.izpack.installer.IzPanel;
import java.awt.LayoutManager2;
import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;

/**
 * A custom IzPack (see http://izpack.org) Panel to read an existing
 * Callimachus configuration and store it in IzPack runtime variables.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class CallimachusConfigurationPanel extends IzPanel {

    /**
     * The constructor.
     *
     * @param parent The parent.
     * @param idata  The installation data.
     */
    public CallimachusConfigurationPanel(InstallerFrame parent, InstallData idata)
    {
        this(parent, idata, new IzPanelLayout());
    }

    /**
     * Creates a new CallimachusConfigurationPanel object. The layout manager
     * may be null because this is an invisible panel (no GUI displayed to the
     * end user).
     *
     * @param parent The parent IzPack installer frame.
     * @param idata  The installer internal data.
     * @param layout layout manager to be used with this IzPanel
     */

    public CallimachusConfigurationPanel(InstallerFrame parent, InstallData idata, LayoutManager2 layout)
    {
        super(parent, idata, layout);
        
        //  TODO
        // Determine the installation directory of this installation run and
        // find out if there is an existing Callimachus here.  If so, read its
        // configuration.
        // NB: See getInstallPath() in com.izforge.izpack.installer.AutomatedInstallData.
        
        // TODO
        // Assign each existing configuration element to variables in IzPack's
        // variable substitution system.
        // NB: See the methods getVariable(), setVariable() and getVariables()
        //     in com.izforge.izpack.installer.AutomatedInstallData
        
        // Go to the next panel; this one should never be displayed to an
        // end user.
        parent.skipPanel();
    }

    /**
     * Indicates wether the panel has been validated or not.
     *
     * @return Always true.
     */
    public boolean isValidated()
    {
        return true;
    }
    
}