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
package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DigestUserEdit extends CalliPage {

	public DigestUserEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public FileUploadForm openPhotoUpload() {
		browser.click(By.cssSelector("#photo label a"));
		browser.focusInModalFrame("photo");
		final DigestUserEdit edit = this;
		return new FileUploadForm(browser) {
			@Override
			public DigestUserEdit uploadAs(String fileName) {
				browser.click(By.id("upload"));
				browser.focusInModalFrame("photo", "save-as___");
				browser.type(By.id("label"), fileName);
				browser.focusInFrame("photo");
				browser.click(By.xpath("//div[@role='dialog']//button[text()='Save']"));
				browser.waitForFrameToClose("photo");
				return edit;
			}
		};
	}

	public CalliPage save() {
		browser.focusInTopWindow();
		browser.click(By.id("save"));
		return page();
	}

	public FolderView delete(String label) {
		browser.focusInTopWindow();
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + label);
		return page(FolderView.class);
	}

}
