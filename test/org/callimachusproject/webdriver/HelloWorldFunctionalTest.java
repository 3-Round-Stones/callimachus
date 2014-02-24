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
import org.openqa.selenium.By;

public class HelloWorldFunctionalTest extends BrowserFunctionalTestCase {
	private static final String DOWNLOAD_URL = "http://callimachus.googlecode.com/files/helloworld.car";

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase
				.suite(HelloWorldFunctionalTest.class);
	}

	public HelloWorldFunctionalTest() {
		super();
	}

	public HelloWorldFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testHelloWorld() throws Exception {
		File car = new AssetDownloader(new File("downloads"))
				.getLocalAsset(DOWNLOAD_URL);
		String archive = page.getCurrentUrl().replaceAll("\\?.*", "?archive");
		page.openCurrentFolder().openImportPage().selectFile(car).importCar();
		verifyHelloWorldApp();
		File ex = new AssetDownloader(getUsername(), getPassword()).downloadAsset(archive, "helloworld.car");
		page.openCurrentFolder().openImportPage().selectFile(ex).importCar();
		ex.delete();
		verifyHelloWorldApp();
	}

	public void verifyHelloWorldApp() {
		browser.click(By.linkText("helloworld.html"));
		browser.waitUntilTextPresent(By.tagName("p"), "Hello, World from HTML!");
		browser.navigateBack();
		browser.click(By.linkText("hello+resource"));
		browser.waitUntilTextPresent(By.tagName("p"),
				"Hello, World from XHTML Template!");
		browser.waitUntilElementPresent(By.linkText("Edit"));
		browser.navigateBack();
	}

}
