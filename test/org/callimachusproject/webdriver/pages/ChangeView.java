package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ChangeView extends CalliPage {

	public ChangeView(WebBrowserDriver driver) {
		super(driver);
	}

	public ChangeView waitUntilText(By locator, String text) {
		browser.waitUntilTextPresent(locator, text);
		return this;
	}

}
