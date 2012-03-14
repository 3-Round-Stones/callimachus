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
import java.util.Properties;

import org.callimachusproject.installer.Configure;

import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;

/**
 * A custom IzPack (see http://izpack.org) Panel to read an existing
 * Callimachus configuration and store it in IzPack runtime variables.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class CallimachusConfigurationPanel extends IzPanel {

	// Instance vars.
	protected InstallData idata;
	boolean debug = false;  // Set to false for production use.

	/**
	 * The constructor.
	 *
	 * @param parent The parent.
	 * @param idata  The installation data.
	 */
	public CallimachusConfigurationPanel(InstallerFrame parent, InstallData idata) {
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

	public CallimachusConfigurationPanel(InstallerFrame parent, InstallData idata, LayoutManager2 layout) {
		super(parent, idata, layout);
		this.idata = idata;
	}

	/**
	 * Read a Callimachus configuration file and set IzPack variables from its
	 * properties.  This method is called when IzPack makes this panel active.
	 *
	 * NB:  ANY IZPACK VARIABLES NAMED IN THIS METHOD MUST BE LISTED IN IZPACK's
	 *      install.xml FILE IN <property> TAGS TO BE ACCESSIBLE TO IZPACK PANELS.
	 *      PROPERTY NAMES MAY THEN BE USED IN IZPACK'S userInputSpec.xml FILE.
	 *      SEE BOTH install.xml AND COMMENTS IN setCallimachusVariables() below.
	 */
	public void panelActivate() {
		try {
			String installPath = idata.getInstallPath();
			Configure configure = new Configure(new File(installPath));

			// Set IzPack variables for callimachus.conf:
			String[] confProperties = {"PORT", "ORIGIN"};
			setCallimachusVariables(configure.getserverConfiguration(), confProperties);

			// Set IzPack variables for mail.properties:
			String[] mailProperties = {"mail.transport.protocol", "mail.from", "mail.smtps.host", "mail.smtps.port", "mail.smtps.auth", "mail.user", "mail.password"};
			setCallimachusVariables(configure.getMailProperties(), mailProperties);
		} catch (IOException e) {
			// This is an unknown error, but there is not much we can do within
			// the scope of IzPack's installation procedure, so just report in
			// case IzPack was run from the command line.
			System.err.println(e.getMessage());
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

	/**
	 * Read a Callimachus configuration file, if present, and set IzPack variables
	 * for each property found in it.
	 *
	 * @param fileName The configuration file to parse.
	 * @param properties A list of property names to convert to IzPack variables.
	 */
	private void setCallimachusVariables(Properties prop, String[] properties) {
		// Get the values of relevant properties and convert them to IzPack
		// variables with the same names but prepended by "callimachus." to
		// avoid namespace conflicts.
		String tempProperty;
		int propertiesLength = properties.length;
		for (int i = 0; i < propertiesLength; i++) {
			if ( prop.getProperty(properties[i]) == null ) {
				tempProperty = "";
			} else {
				tempProperty = prop.getProperty(properties[i]);
			}
			idata.setVariable("callimachus." + properties[i], tempProperty);
		}
	}

}