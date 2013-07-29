package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class FolderCreate extends CalliPage {

	public FolderCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public FolderCreate with(String folderName) {
		driver.type(By.id("label"), folderName);
		return this;
	}

	public FolderView create() {
		driver.click(By.cssSelector("button.btn.btn-success"));
		return page(FolderView.class);
	}

}
