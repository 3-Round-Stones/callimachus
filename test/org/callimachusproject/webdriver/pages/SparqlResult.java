package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class SparqlResult extends CalliPage {

	public SparqlResult(WebBrowserDriver driver) {
		super(driver);
	}

	public SparqlResult waitUntilResult(String text) {
		browser.waitUntilTextPresent(By.tagName("td"), text);
		return this;
	}

}
