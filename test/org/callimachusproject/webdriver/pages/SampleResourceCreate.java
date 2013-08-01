package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class SampleResourceCreate extends CalliPage {

	public SampleResourceCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public SampleResourceCreate with(String label, String comment) {
		driver.type(By.id("label"), label);
		driver.type(By.id("comment"), comment);
		return this;
	}

	public CalliPage createAs() {
		driver.click(By.cssSelector("button.btn-success"));
		driver.focusInFrame("save-as___");
		driver.focusInTopWindow();
		driver.click(By.xpath("(//button[@type='button'])[2]"));
		driver.waitForScript();
		return page();
	}

}
