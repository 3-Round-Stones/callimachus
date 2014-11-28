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
package org.callimachusproject.webdriver.pages;

import java.io.File;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ImportPage extends CalliPage {

	public ImportPage(WebBrowserDriver driver) {
		super(driver);
	}

	public ImportPage selectFile(File file) {
		browser.sendFileName(By.id("file"), file);
		return this;
	}

	public ImportPage replaceContents() {
		browser.click(By.id("replace"));
		return this;
	}

	public CalliPage importCar() {
		browser.click(By.id("import"));
		return page();
	}

}
