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

	public ConceptCreate openNarrowDialogue() {
		driver.click(By.cssSelector("#narrower label.control-label a"));
		driver.waitForScript();
		driver.focusInFrame("narrower");
		driver.waitForScript();
		final ConceptEdit edit = this;
		return new ConceptCreate(driver) {
			@Override
			public ConceptEdit create() {
				super.create();
				driver.focusInTopWindow();
				return edit;
			}
		};
	}

}
