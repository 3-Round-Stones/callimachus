package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class FolderEdit extends CalliPage {

	public FolderEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public FolderEdit waitUntilTitle(String title) {
		super.waitUntilTitle(title);
		return this;
	}

	public CalliPage delete() {
		driver.click(By.id("delete"));
		driver.confirm("Are you sure you want to delete this folder and all the contents of this folder");
		return page();
	}

}
