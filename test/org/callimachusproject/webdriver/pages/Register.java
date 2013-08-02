package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class Register extends CalliPage {

	public Register(WebBrowserDriver driver) {
		super(driver);
	}

	public Register with(String username, String password, String fullname,
			String email) {
		driver.type(By.id("fullname"), fullname);
		driver.type(By.id("email"), email);
		driver.type(By.id("username"), username);
		driver.type(By.id("password"), password);
		return this;
	}

	public Login signup() {
		driver.click(By.id("signup"));
		return page(Login.class);
	}
}
