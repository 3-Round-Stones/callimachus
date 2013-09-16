package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class PurlEdit extends CalliPage {

	public PurlEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String purlName) {
		browser.click(By.cssSelector("button.btn.btn-danger"));
		browser.confirm("Are you sure you want to delete " + purlName);
		return page();
	}

}
