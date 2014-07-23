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

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ClassView extends CalliPage {

	public ClassView(WebBrowserDriver driver) {
		super(driver);
	}

	@Override
	public ClassView waitUntilTitle(String title) {
		super.waitUntilTitle(title);
		return this;
	}

	public ClassIndex openIndex(String label) {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("button.navbar-btn.dropdown-toggle"));
		browser.click(By.linkText(label + " resources"));
		return page(ClassIndex.class);
	}

}
