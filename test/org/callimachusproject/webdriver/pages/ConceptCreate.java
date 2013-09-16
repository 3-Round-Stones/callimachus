package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ConceptCreate extends CalliPage {

	public ConceptCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public ConceptCreate with(String conceptName, String conceptLabel,
			String conceptDefinition, String conceptExample) {
		browser.type(By.id("label"), conceptLabel);
		browser.type(By.id("definition"), conceptDefinition);
		browser.type(By.id("example"), conceptExample);
		return this;
	}

	public CalliPage create() {
		browser.click(By.id("create"));
		return page();
	}

}
