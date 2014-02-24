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

public class ClassEdit extends CalliPage {

	public ClassEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public ClassEdit with(String label, String comment) {
		browser.type(By.id("label"), label);
		browser.type(By.id("comment"), comment);
		return this;
	}

	public ClassView create() {
		browser.submit(By.id("create"));
		return page(ClassView.class);
	}

	public CalliPage delete(String conceptLabel) {
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + conceptLabel);
		return page();
	}

	public TextEditor openCreateTemplate() {
		browser.click(By.cssSelector("#create label a"));
		browser.waitForScript();
		browser.focusInModalFrame("template-for-creating");
		browser.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("template-for-creating", browser) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				browser.focusInTopWindow();
				return edit;
			}
		};
	}

	public TextEditor openViewTemplate() {
		browser.click(By.cssSelector("#view label a"));
		browser.waitForScript();
		browser.focusInModalFrame("template-for-viewing");
		browser.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("template-for-viewing", browser) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				browser.focusInTopWindow();
				return edit;
			}
		};
	}

	public TextEditor openEditTemplate() {
		browser.click(By.cssSelector("#edit label a"));
		browser.waitForScript();
		browser.focusInModalFrame("template-for-editing");
		browser.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("template-for-editing", browser) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				browser.focusInTopWindow();
				return edit;
			}
		};
	}

}
