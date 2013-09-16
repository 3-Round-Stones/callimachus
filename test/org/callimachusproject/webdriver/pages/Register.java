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
