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
