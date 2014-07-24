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

public class TextEditor extends CalliPage {
	private String topFrameName;

	public TextEditor(WebBrowserDriver driver) {
		super(driver);
	}

	public TextEditor(String topFrameName, WebBrowserDriver driver) {
		super(driver);
		this.topFrameName = topFrameName;
	}

	public TextEditor setText(String text) {
		browser.focusInFrame(topFrameName, "editor-iframe");
		browser.setStyle(By.tagName("textarea"), "opacity", "1");
		return clear().type(text).end();
	}

	private TextEditor clear() {
		browser.focusInFrame(topFrameName, "editor-iframe");
		// shift/control does not appear to work in IE
		CharSequence[] keys = new CharSequence[1024];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Keys.BACK_SPACE;
		}
		browser.sendKeys(By.tagName("textarea"), keys);
		return this;
	}

	private TextEditor type(String text) {
		browser.focusInFrame(topFrameName, "editor-iframe");
		browser.sendKeys(By.tagName("textarea"), text);
		return this;
	}

	private TextEditor end() {
		browser.focusInFrame(topFrameName, "editor-iframe");
		// shift/control does not appear to work in IE
		CharSequence[] keys = new CharSequence[1024];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Keys.DELETE;
		}
		browser.sendKeys(By.tagName("textarea"), keys);
		return this;
	}

	public CalliPage saveAs(String name) {
		browser.focusInFrame(topFrameName);
		browser.type(By.id("label"), name);
		browser.click(By.cssSelector("button.btn-success"));
		browser.waitForFrameToClose(topFrameName);
		return page();
	}

	public CalliPage delete() {
		browser.click(By.cssSelector("button.btn.btn-danger"));
		browser.confirm("Are you sure you want to delete");
		browser.waitForScript();
		return page();
	}

}
