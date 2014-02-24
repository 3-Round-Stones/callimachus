/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.webdriver;

import java.io.File;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.AssetDownloader;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.DatasourceView;
import org.callimachusproject.webdriver.pages.FolderView;
import org.openqa.selenium.By;

public class SchoolWardFunctionalTest extends BrowserFunctionalTestCase {
	private static final String DOWNLOAD_URL = "http://callimachus.googlecode.com/files/schools.car";

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase
				.suite(SchoolWardFunctionalTest.class);
	}

	public SchoolWardFunctionalTest() {
		super();
	}

	public SchoolWardFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testSchoolWard() throws Exception {
		File car = new AssetDownloader(new File("downloads"))
				.getLocalAsset(DOWNLOAD_URL);
		FolderView folder = page.openCurrentFolder().openImportPage()
				.selectFile(car).importCar().openCurrentFolder();
		String archive = folder.getCurrentUrl().replaceAll("\\?.*", "?archive");
		File ex = new AssetDownloader(getUsername(), getPassword()).downloadAsset(archive, "schools.car");
		folder.openImportPage().selectFile(ex).importCar();
		ex.delete();
		browser.click(By.linkText("Bartley Green"));
		browser.focusInTopWindow();
		browser.click(By
				.linkText("Bartley Green School A Specialist Technology and Sports College"));
		browser.focusInTopWindow();
		String uri = browser.getCurrentUrl().replace("?view", "");
		browser.click(By.linkText("Edit"));
		browser.focusInTopWindow();
		browser.type(By.id("name"), "Bartley Green School");
		browser.type(By.id("comment"),
				"A Specialist Technology and Sports College");
		browser.click(By.id("save"));
		browser.focusInTopWindow();
		browser.waitUntilTextPresent(By.tagName("p"),
				"A Specialist Technology and Sports College");
		page.open("/sparql", DatasourceView.class).query("DELETE WHERE { <" + uri + "> ?p ?o }").execute();
		browser.focusInTopWindow();
	}

}
