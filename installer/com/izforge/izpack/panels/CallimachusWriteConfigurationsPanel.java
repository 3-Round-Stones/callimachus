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
import java.awt.LayoutManager2;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.callimachusproject.installer.Configure;

import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;

/**
 * A custom IzPack (see http://izpack.org) Panel to write Callimachus
 * configuration files.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class CallimachusWriteConfigurationsPanel extends IzPanel {
    
    // Instance vars.
    protected InstallData idata;
    
    /**
     * The constructor.
     *
     * @param parent The parent.
     * @param idata  The installation data.
     */
    public CallimachusWriteConfigurationsPanel(InstallerFrame parent, InstallData idata) {
        // Use a null layout manager because this is an invisible panel (no GUI
        // displayed to the end user).
        this(parent, idata, null);
        this.idata = idata;
    }

    /**
     * Creates a new CallimachusConfigurationPanel object.
     *
     * @param parent The parent IzPack installer frame.
     * @param idata  The installer internal data.
     * @param layout layout manager to be used with this IzPanel
     */

    public CallimachusWriteConfigurationsPanel(InstallerFrame parent, InstallData idata, LayoutManager2 layout) {
        super(parent, idata, layout);
        this.idata = idata;
    }

   /**
    * Write Callimachus configuration files from IzPack variables.
		* This method is called when IzPack makes this panel active.
    */
    public void panelActivate() {
        String installPath = idata.getInstallPath();
        Configure configure = new Configure(new File(installPath));
        
    	String origin = idata.getVariable("callimachus.ORIGIN");
		try {
        	// Write Callimachus configuration file.
        	Properties confProperties = configure.getServerConfiguration();
            // NB: Ensure that these var names are correct in install.xml, userInputSpec.xml
            confProperties.setProperty("PORT", idata.getVariable("callimachus.PORT") );
            confProperties.setProperty("ORIGIN", origin );
    		configure.setServerConfiguration(confProperties);
        } catch (IOException e) {
            // This is an unknown error, but there is not much we can do within
            // the scope of IzPack's installation procedure, so just report in
            // case IzPack was run from the command line.
            // TODO: Perhaps stop the installation process when this happens??
        	e.printStackTrace();
			System.exit(1);
        }
        
    	try {
            // Write Callimachus mail properties file.\
        	Properties mailProperties = configure.getMailProperties();
            // NB: Ensure that these var names are correct in install.xml, userInputSpec.xml
            mailProperties.setProperty("mail.transport.protocol", idata.getVariable("callimachus.mail.transport.protocol") );
            mailProperties.setProperty("mail.from", idata.getVariable("callimachus.mail.from") );
            mailProperties.setProperty("mail.smtps.host", idata.getVariable("callimachus.mail.smtps.host") );
            mailProperties.setProperty("mail.smtps.port", idata.getVariable("callimachus.mail.smtps.port") );
            mailProperties.setProperty("mail.smtps.auth", idata.getVariable("callimachus.mail.smtps.auth") );
            mailProperties.setProperty("mail.user", idata.getVariable("callimachus.mail.user") );
            mailProperties.setProperty("mail.password", idata.getVariable("callimachus.mail.password") );
    		configure.setMailProperties(mailProperties);
        } catch (IOException e) {
            // This is an unknown error, but there is not much we can do within
            // the scope of IzPack's installation procedure, so just report in
            // case IzPack was run from the command line.
            // TODO: Perhaps stop the installation process when this happens??
            e.printStackTrace();
			System.exit(1);
        }

    	try {
    		boolean running = configure.isServerRunning();
			if (running) {
    			if (!configure.stopServer()) {
    				System.err.println("Server must be shutdown to continue");
    				System.exit(1);
    			}
    		}
			configure.setLoggingProperties(configure.getLoggingProperties());
			URL config = configure.getRepositoryConfigTemplates().values().iterator().next();
			configure.connect(config, null);
			configure.createOrigin(origin);
			configure.disconnect();
			if (running) {
				boolean started = configure.startServer();
				if (started && configure.isWebBrowserSupported()) {
					configure.openWebBrowser(origin + "/");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
        
        // Go to the next panel; this one should never be displayed to an
        // end user.
        parent.skipPanel();
    }

    /**
     * Indicates wether the panel has been validated or not.
     *
     * @return Always true.
     */
    public boolean isValidated() {
        return true;
    }
       
}