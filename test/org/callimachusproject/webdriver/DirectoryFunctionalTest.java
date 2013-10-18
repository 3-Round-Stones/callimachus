package org.callimachusproject.webdriver;

import java.io.File;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.AssetDownloader;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.CalliPage;
import org.callimachusproject.webdriver.pages.ClassView;
import org.callimachusproject.webdriver.pages.FolderView;
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
		FolderView folder = page.openCurrentFolder().openImportPage()
				.selectFile(car).importCar().openCurrentFolder();
		String archive = folder.getCurrentUrl().replaceAll("\\?.*", "?archive");
		File ex = new AssetDownloader(getUsername(), getPassword()).downloadAsset(archive, "directory.car");
		folder.openImportPage().selectFile(ex).importCar();
		ex.delete();
		page.openCurrentFolder();
		browser.click(By.linkText("Organization"));
		page.page(ClassView.class).createANew("Organization", CalliPage.class);
		browser.type(By.id("legal"), "3 Round Stones Inc.");
		browser.select(By.id("orgtype"), "Commercial");
		browser.type(By.id("url"), "http://3roundstones.com/");
		browser.select(By.xpath("//div[@id='adr']//select"), "United States");
		browser.click(By.cssSelector("button#create"));
		browser.focusInFrame("save-as___");
		browser.focusInTopWindow();
		browser.click(By.xpath("//div[@role='dialog']//button[1]"));
		browser.focusInTopWindow();
		browser.click(By.linkText("Edit"));
		browser.type(By.xpath("//div[@id='adr']//input[@placeholder='state or province']"), "DC");
		browser.click(By.cssSelector("button[type=submit]"));
	}

}
