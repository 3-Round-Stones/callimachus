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

public class SignIn extends CalliPage {

	public SignIn(WebBrowserDriver driver) {
		super(driver);
	}

	public Login loginWithDigest() {
		browser.click(By.linkText("Sign in with your email address and a site password"));
		browser.waitForScript();
		return page(Login.class);
	}

	public Register registerWithDigest() {
		browser.click(By.linkText("Sign in with your email address and a site password"));
		browser.waitForScript();
		return page(Register.class);
	}

}
