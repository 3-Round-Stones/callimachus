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
		driver.type(By.id("username"), username);
		driver.type(By.id("password"), password);
		return this;
	}

	public CalliPage login() {
		driver.click(By.cssSelector("button.btn.btn-primary"));
		driver.waitForScript();
		return page();
	}

}
