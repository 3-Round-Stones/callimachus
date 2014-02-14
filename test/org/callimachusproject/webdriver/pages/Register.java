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

public class Register extends CalliPage {

	public Register(WebBrowserDriver driver) {
		super(driver);
	}

	public Register with(String username, String password, String fullname,
			String email) {
		browser.type(By.id("fullname"), fullname);
		browser.type(By.id("email"), email);
		browser.type(By.id("username"), username);
		browser.type(By.id("password"), password);
		return this;
	}

	public Login signup() {
		browser.click(By.id("signup"));
		return page(Login.class);
	}
}
