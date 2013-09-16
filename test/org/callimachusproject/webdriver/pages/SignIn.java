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
