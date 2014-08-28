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
import org.callimachusproject.webdriver.pages.FolderView;
import org.callimachusproject.webdriver.pages.SampleResourceCreate;
import org.openqa.selenium.By;

public class MeetingNotesFunctionalTest extends BrowserFunctionalTestCase {
	private static final String DOWNLOAD_URL = "https://github.com/3-Round-Stones/meeting-notes/archive/master.zip";

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
		folder.openImportPage().selectFile(ex).replaceContents().importCar();
		ex.delete();
		logger.info("Creating Journal");
		page.openCurrentFolder().openCreateHref("Journal", SampleResourceCreate.class)
				.with("R & D", "Research and Development journal").create();
		browser.focusInTopWindow();
		logger.info("Creating note");
		browser.click(By.linkText("Create a new note"));
		browser.focusInTopWindow();
		browser.type(By.id("comment"), "Testing callimachus");
		browser.click(By.cssSelector("button.btn-success"));
		browser.focusInTopWindow();
		browser.click(By.linkText("Edit"));
		browser.focusInTopWindow();
		logger.info("Creating tag");
		browser.type(By.xpath("//div[label/@for='topics']//input"), "Callimachus Tag");
		browser.focusInModalFrame("topics");
		browser.type(By.id("comment"), "Anything about Callimachus");
		browser.click(By.xpath("//button[text()='Add']"));
		browser.waitForFrameToClose("topics");
		browser.click(By.xpath("//button[text()='Save']"));
		browser.focusInTopWindow();
		browser.click(By.linkText("Callimachus Tag"));
	}

}
