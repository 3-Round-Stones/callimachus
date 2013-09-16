package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class GroupCreate extends CalliPage {

	public GroupCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public GroupCreate with(String label, String comment) {
		browser.type(By.id("label"), label);
		browser.type(By.id("comment"), comment);
		return this;
	}

	public CalliPage create() {
		browser.click(By.id("create"));
		return page();
	}

}
