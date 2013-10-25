package org.callimachusproject.webdriver;

import java.io.File;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.AssetDownloader;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.ClassView;
import org.callimachusproject.webdriver.pages.FolderView;
import org.callimachusproject.webdriver.pages.SampleResourceCreate;
import org.openqa.selenium.By;

public class MeetingNotesFunctionalTest extends BrowserFunctionalTestCase {
	private static final String DOWNLOAD_URL = "http://callimachus.googlecode.com/files/notes.car";

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase
				.suite(MeetingNotesFunctionalTest.class);
	}

	public MeetingNotesFunctionalTest() {
		super();
	}

	public MeetingNotesFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testMeetingNotes() throws Exception {
		File car = new AssetDownloader(new File("downloads"))
				.getLocalAsset(DOWNLOAD_URL);
		FolderView folder = page.openCurrentFolder().openImportPage()
				.selectFile(car).importCar().openCurrentFolder();
		String archive = folder.getCurrentUrl().replaceAll("\\?.*", "?archive");
		File ex = new AssetDownloader(getUsername(), getPassword()).downloadAsset(archive, "meeting.car");
		folder.openImportPage().selectFile(ex).importCar();
		ex.delete();
		logger.info("Creating Journal");
		browser.click(By.linkText("Journal"));
		page.page(ClassView.class)
				.createANew("Journal", SampleResourceCreate.class)
				.with("R & D", "Research and Development journal").createAs();
		browser.focusInTopWindow();
		logger.info("Creating note");
		browser.click(By.linkText("Create a new note"));
		browser.focusInTopWindow();
		browser.type(By.id("comment"), "Testing callimachus");
		browser.click(By.cssSelector("button[type=submit]"));
		browser.focusInTopWindow();
		browser.click(By.linkText("Edit"));
		browser.focusInTopWindow();
		logger.info("Creating tag");
		browser.click(By.xpath("//label[@for='topic']/a"));
		browser.focusInFrame("topics");
		browser.type(By.id("label"), "Callimachus Tag");
		browser.type(By.id("comment"), "Anything about Callimachus");
		browser.click(By.cssSelector("button[type=submit]"));
		browser.waitForFrameToClose("topics");
		browser.click(By.cssSelector("button[type=submit]"));
		browser.focusInTopWindow();
		browser.click(By.linkText("Callimachus Tag"));
	}

}
