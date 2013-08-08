package org.callimachusproject.webdriver.pages;

import java.io.File;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public abstract class FileUploadForm extends CalliPage {

	public FileUploadForm(WebBrowserDriver driver) {
		super(driver);
	}

	public abstract CalliPage uploadAs(String fileName);

	public FileUploadForm selectFile(File file) {
		driver.sendKeys(By.id("file"), file.getAbsolutePath());
		return this;
	}

}
