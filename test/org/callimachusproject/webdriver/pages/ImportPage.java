package org.callimachusproject.webdriver.pages;

import java.io.File;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ImportPage extends CalliPage {

	public ImportPage(WebBrowserDriver driver) {
		super(driver);
	}

	public ImportPage selectFile(File file) {
		browser.sendFileName(By.id("file"), file);
		return this;
	}

	public CalliPage importCar() {
		browser.focusInTopWindow();
		browser.click(By.id("import"));
		return page();
	}

}
