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

public class ConceptCreate extends CalliPage {

	public ConceptCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public ConceptCreate with(String conceptName, String conceptLabel,
			String conceptDefinition, String conceptExample) {
		browser.type(By.id("label"), conceptLabel);
		browser.type(By.id("definition"), conceptDefinition);
		browser.type(By.id("example"), conceptExample);
		return this;
	}

	public CalliPage create() {
		browser.click(By.id("create"));
		return page();
	}

}
