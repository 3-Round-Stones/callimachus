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
import org.openqa.selenium.Keys;

public class DocEditor extends CalliPage {

	public DocEditor(WebBrowserDriver driver) {
		super(driver);
	}

	public DocEditor clear() {
		browser.focusInFrameIndex(0, 0);
		browser.innerHTML(By.cssSelector("body[contenteditable]"), "");
		return this;
	}

	public DocEditor appendHTML(String html) {
		browser.focusInFrameIndex(0, 0);
		browser.appendHTML(By.cssSelector("body[contenteditable]"), html);
		return this;
	}

	public DocEditor end() {
		browser.focusInFrameIndex(0, 0);
		browser.sendKeys(Keys.END);
		browser.sendKeys(Keys.ARROW_RIGHT);
		return this;
	}

	public DocEditor type(String text) {
		browser.focusInFrameIndex(0, 0);
		browser.sendKeys(text);
		return this;
	}

	public DocEditor heading1() {
		browser.focusInFrameIndex(0);
		browser.click(By.cssSelector(".cke_combo__format a.cke_combo_button"));
		browser.focusInFrameIndex(0, 1);
		browser.click(By.partialLinkText("Heading 1"));
		return this;
	}

	public CalliPage saveAs(String articleName) {
		browser.focusInTopWindow();
		browser.type(By.id("label"), articleName);
		browser.click(By.cssSelector("button.btn-success"));
		return page();
	}

	public CalliPage delete() {
		browser.click(By.cssSelector("button.btn.btn-danger"));
		browser.confirm("Are you sure you want to delete");
		return page();
	}

}
