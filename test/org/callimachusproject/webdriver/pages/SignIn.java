package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class SignIn extends CalliPage {

	public SignIn(WebBrowserDriver driver) {
		super(driver);
	}

	public Login loginWithDigest() {
		driver.click(By.linkText("Sign in with your email address and a site password"));
		driver.waitForScript();
		return page(Login.class);
	}

	public Register registerWithDigest() {
		driver.click(By.linkText("Sign in with your email address and a site password"));
		driver.waitForScript();
		return page(Register.class);
	}

}
