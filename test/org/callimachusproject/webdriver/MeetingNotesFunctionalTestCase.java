package org.callimachusproject.webdriver;

import java.io.File;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.AssetDownloader;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.ClassView;
import org.callimachusproject.webdriver.pages.SampleResourceCreate;
import org.openqa.selenium.By;

public class MeetingNotesFunctionalTestCase extends BrowserFunctionalTestCase {
	private static final String DOWNLOAD_URL = "http://callimachus.googlecode.com/files/notes.car";

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase
				.suite(MeetingNotesFunctionalTestCase.class);
	}

	public MeetingNotesFunctionalTestCase() {
		super();
	}

	public MeetingNotesFunctionalTestCase(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testMeetingNotes() throws Exception {
		File car = new AssetDownloader(new File("downloads")).download(DOWNLOAD_URL);
		page.openCurrentFolder().openImportPage().selectFile(car).importCar();
		logger.info("Creating Journal");
		browser.click(By.linkText("Journal"));
		page.page(ClassView.class)
				.createANew("Journal", SampleResourceCreate.class)
				.with("R & D", "Research and Development journal").createAs();
		browser.focusInTopWindow();
		logger.info("Creating note");
		browser.click(By.cssSelector("i.icon-cog"));
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
