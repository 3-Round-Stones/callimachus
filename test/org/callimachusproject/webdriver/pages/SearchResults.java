package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class SearchResults extends CalliPage {

	public SearchResults(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage openResult(String label) {
		driver.click(By.linkText(label));
		return page();
	}

}
