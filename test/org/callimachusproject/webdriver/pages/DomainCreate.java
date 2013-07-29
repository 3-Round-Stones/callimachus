package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DomainCreate extends CalliPage {

	public DomainCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public DomainCreate with(String label, String comment) {
		driver.type(By.id("label"), label);
		driver.type(By.id("comment"), comment);
		return this;
	}

	public CalliPage create() {
		driver.click(By.id("create"));
		return page();
	}

}
