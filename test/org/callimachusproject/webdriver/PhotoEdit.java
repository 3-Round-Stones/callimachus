package org.callimachusproject.webdriver;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.callimachusproject.webdriver.pages.CalliPage;
import org.openqa.selenium.By;

public class PhotoEdit extends CalliPage {

	public PhotoEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		driver.click(By.id("delete"));
		driver.confirm("Are you sure you want to delete " + label);
		return page();
	}

}
