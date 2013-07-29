package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ConceptCreate extends CalliPage {

	public ConceptCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public ConceptCreate with(String conceptName, String conceptLabel,
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

}
