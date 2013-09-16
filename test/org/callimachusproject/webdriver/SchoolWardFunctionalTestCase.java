package org.callimachusproject.webdriver;

import java.io.File;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.AssetDownloader;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.openqa.selenium.By;

public class SchoolWardFunctionalTestCase extends BrowserFunctionalTestCase {
	private static final String DOWNLOAD_URL = "http://callimachus.googlecode.com/files/schools.car";

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase
				.suite(SchoolWardFunctionalTestCase.class);
	}

	public SchoolWardFunctionalTestCase() {
		super();
	}

	public SchoolWardFunctionalTestCase(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testSchoolWard() throws Exception {
		File car = new AssetDownloader(new File("downloads")).download(DOWNLOAD_URL);
		page.openCurrentFolder().openImportPage().selectFile(car).importCar();
		browser.click(By.linkText("Bartley Green"));
		browser.focusInTopWindow();
		browser.click(By.linkText("Bartley Green School A Specialist Technology and Sports College"));
		browser.focusInTopWindow();
		browser.click(By.linkText("Edit"));
		browser.focusInTopWindow();
		browser.type(By.id("name"), "Bartley Green School");
		browser.type(By.id("comment"), "A Specialist Technology and Sports College");
		browser.click(By.id("save"));
		browser.focusInTopWindow();
		browser.waitUntilTextPresent(By.tagName("p"), "A Specialist Technology and Sports College");
	}

}
