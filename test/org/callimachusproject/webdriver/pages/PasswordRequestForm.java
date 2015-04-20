package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class PasswordRequestForm extends CalliPage {

	public PasswordRequestForm(WebBrowserDriver browser) {
		super(browser);
	}

	public PasswordRequestForm with(String email) {
		browser.type(By.id("email"), email);
		return this;
	}

	public PasswordRequestForm request() {
		browser.click(By.cssSelector("button.btn.btn-primary"));
		browser.waitForScript();
		return this;
	}

}
