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

public class GroupEdit extends CalliPage {

	public GroupEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + label);
		return page();
	}

	public CalliPage save() {
		browser.click(By.id("save"));
		return page();
	}

	public InviteUser openInviteUser(String email) {
		browser.type(By.xpath("//div[select/@id='members']//input"), email);
		browser.focusInModalFrame("members");
		final GroupEdit edit = this;
		return new InviteUser(browser) {
			@Override
			public GroupEdit invite() {
				browser.click(By.id("invite"));
				browser.waitForFrameToClose("members");
				return edit;
			}
		};
	}

}
