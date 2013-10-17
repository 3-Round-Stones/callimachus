package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class SampleResourceCreate extends CalliPage {

	public SampleResourceCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public SampleResourceCreate with(String label, String comment) {
		browser.type(By.id("label"), label);
		browser.type(By.id("comment"), comment);
		return this;
	}

	public CalliPage createAs() {
		browser.click(By.cssSelector("button.btn-success"));
		browser.focusInFrame("save-as___");
		browser.focusInTopWindow();
		browser.click(By.xpath("//div[@role='dialog']//button[1]"));
		browser.waitForScript();
		return page();
	}

}
