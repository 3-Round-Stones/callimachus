package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DomainEdit extends CalliPage {

	public DomainEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + label);
		return page();
	}

}
