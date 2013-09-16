package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ConceptEdit extends CalliPage {

	public ConceptEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + label);
		return page();
	}

	public ConceptEdit definition(String value) {
		browser.type(By.id("definition"), value);
		return this;
	}

	public CalliPage save() {
		browser.click(By.id("save"));
		return page();
	}

	public ConceptCreate openNarrowDialogue() {
		browser.click(By.cssSelector("#narrower label.control-label a"));
		browser.waitForScript();
		browser.focusInFrame("narrower");
		browser.waitForScript();
		final ConceptEdit edit = this;
		return new ConceptCreate(browser) {
			@Override
			public ConceptEdit create() {
				browser.click(By.id("create"));
				browser.waitForFrameToClose("narrower");
				return edit;
			}
		};
	}

}
