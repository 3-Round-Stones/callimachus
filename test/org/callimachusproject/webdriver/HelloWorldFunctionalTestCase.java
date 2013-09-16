package org.callimachusproject.webdriver;

import java.io.File;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.AssetDownloader;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.openqa.selenium.By;

public class HelloWorldFunctionalTestCase extends BrowserFunctionalTestCase {
	private static final String DOWNLOAD_URL = "http://callimachus.googlecode.com/files/helloworld.car";

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase
				.suite(HelloWorldFunctionalTestCase.class);
	}

	public HelloWorldFunctionalTestCase() {
		super();
	}

	public HelloWorldFunctionalTestCase(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testHelloWorld() throws Exception {
		File car = new AssetDownloader(new File("downloads")).download(DOWNLOAD_URL);
		page.openCurrentFolder().openImportPage().selectFile(car).importCar();
		browser.click(By.linkText("helloworld.html"));
		browser.waitUntilTextPresent(By.tagName("p"), "Hello, World from HTML!");
		browser.navigateBack();
		browser.click(By.linkText("hello+resource"));
		browser.waitUntilTextPresent(By.tagName("p"), "Hello, World from XHTML Template!");
		browser.waitUntilElementPresent(By.linkText("Edit"));
		browser.navigateBack();
	}

}
