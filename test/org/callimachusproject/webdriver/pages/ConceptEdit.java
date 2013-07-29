package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ConceptEdit extends CalliPage {

	public ConceptEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public ConceptEdit with(String conceptName, String conceptLabel,
			String conceptDefinition, String conceptExample) {
		driver.type(By.id("label"), conceptLabel);
		driver.type(By.id("definition"), conceptDefinition);
		driver.type(By.id("example"), conceptExample);
		return this;
	}

	public CalliPage create() {
		driver.click(By.id("create"));
		return page();
	}

	public CalliPage delete(String conceptLabel) {
		driver.click(By.id("delete"));
		driver.confirm("Are you sure you want to delete " + conceptLabel);
		return page();
	}

}
