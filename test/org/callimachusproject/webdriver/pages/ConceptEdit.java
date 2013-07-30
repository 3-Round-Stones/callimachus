package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ConceptEdit extends CalliPage {

	public ConceptEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		driver.click(By.id("delete"));
		driver.confirm("Are you sure you want to delete " + label);
		return page();
	}

	public ConceptEdit definition(String value) {
		driver.type(By.id("definition"), value);
		return this;
	}

	public CalliPage save() {
		driver.click(By.id("save"));
		return page();
	}

}
