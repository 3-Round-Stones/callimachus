package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ClassIndex extends CalliPage {

	public ClassIndex(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage openResource(String label) {
		driver.click(By.linkText(label));
		return page();
	}

}
