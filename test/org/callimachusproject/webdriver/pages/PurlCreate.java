package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class PurlCreate extends CalliPage {

	public PurlCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public PurlCreate with(String purlName, String purlComment,
			String purlType, String purlTarget, String purlCache) {
		driver.type(By.id("local"), purlName);
		driver.type(By.id("comment"), purlComment);
		driver.select(By.id("type"), purlType);
		driver.type(By.id("pattern"), purlTarget);
		driver.type(By.id("cache"), purlCache);
		return this;
	}

	public CalliPage create() {
		driver.click(By.id("create"));
		return page();
	}

}
