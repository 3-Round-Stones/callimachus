package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DiscussionPage extends CalliPage {

	public DiscussionPage(WebBrowserDriver driver) {
		super(driver);
	}

	public DiscussionPage with(String msg) {
		browser.type(By.cssSelector("form textarea"), msg);
		return this;
	}

	public DiscussionPage post() {
		browser.click(By.id("send"));
		return page(DiscussionPage.class);
	}

	public DiscussionPage waitUntilText(By locator, String text) {
		browser.waitUntilTextPresent(locator, text);
		return this;
	}

}
