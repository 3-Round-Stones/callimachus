/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
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
import org.callimachusproject.webdriver.pages.CalliPage;
import org.openqa.selenium.By;

public class DirectoryFunctionalTest extends BrowserFunctionalTestCase {
	private static final String DOWNLOAD_URL = "https://github.com/3-Round-Stones/directory/releases/download/v1.6/directory-1.6.car";

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase
				.suite(DirectoryFunctionalTest.class);
	}

	public DirectoryFunctionalTest() {
		super();
	}

	public DirectoryFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testDirectory() throws Exception {
		File car = new AssetDownloader(new File("downloads"))
				.getLocalAsset(DOWNLOAD_URL);
		page.openCurrentFolder().openImportPage().selectFile(car).importCar();
		page.openCurrentFolder().openCreateHref("Organization", CalliPage.class);
		browser.type(By.id("legal"), "3 Round Stones Inc.");
		browser.select(By.id("orgtype"), "Commercial");
		browser.type(By.id("url"), "http://3roundstones.com/");
		browser.select(By.xpath("//div[@id='adr']//select"), "United States");
		browser.click(By.cssSelector("button#create"));
		browser.click(By.linkText("Edit"));
		browser.type(By.xpath("//div[@id='adr']//input[@placeholder='state or province']"), "DC");
		browser.click(By.cssSelector("button.btn-success"));
	}

}
