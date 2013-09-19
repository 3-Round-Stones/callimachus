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
