package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DomainCreate extends CalliPage {

	public DomainCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public DomainCreate with(String label, String comment) {
		browser.type(By.id("label"), label);
		browser.type(By.id("comment"), comment);
		return this;
	}

	public CalliPage create() {
		browser.click(By.id("create"));
		return page();
	}

}
