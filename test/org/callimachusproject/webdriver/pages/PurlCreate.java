package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class PurlCreate extends CalliPage {

	public PurlCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public PurlCreate with(String purlName, String purlComment,
			String purlType, String purlTarget, String purlCache) {
		browser.type(By.id("local"), purlName);
		browser.type(By.id("comment"), purlComment);
		browser.select(By.id("type"), purlType);
		browser.type(By.cssSelector("textarea.pattern"), purlTarget);
		browser.type(By.id("cache"), purlCache);
		return page(PurlCreate.class);
	}

	public CalliPage create() {
		browser.click(By.id("create"));
		return page();
	}

}
