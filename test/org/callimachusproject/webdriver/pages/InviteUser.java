package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public abstract class InviteUser extends CalliPage {

	public InviteUser(WebBrowserDriver driver) {
		super(driver);
	}

	public InviteUser with(String fullname, String email) {
		driver.type(By.id("label"), fullname);
		driver.type(By.id("email"), email);
		return this;
	}

	public InviteUser subject(String subject) {
		driver.type(By.id("subject"), subject);
		return this;
	}

	public abstract GroupEdit invite();
}
