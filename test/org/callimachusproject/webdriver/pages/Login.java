package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class Login extends CalliPage {

	public Login(WebBrowserDriver driver) {
		super(driver);
	}

	public Login with(String username, char[] password) {
		return with(username, new String(password));
	}

	public Login with(String username, String password) {
		browser.type(By.id("username"), username);
		browser.type(By.id("password"), password);
		return this;
	}

	public CalliPage login() {
		browser.click(By.cssSelector("button.btn.btn-primary"));
		browser.waitForScript();
		return page();
	}

}
