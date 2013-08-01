package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class SampleResourceEdit extends CalliPage {

	public SampleResourceEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		driver.click(By.cssSelector("button.btn-danger"));
		driver.confirm("Are you sure you want to delete " + label);
		return page();
	}

}
