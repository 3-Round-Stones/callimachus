package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DigestUserEdit extends CalliPage {

	public DigestUserEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public FileUploadForm openPhotoUpload() {
		driver.click(By.cssSelector("#photo label.control-label a"));
		driver.waitForScript();
		driver.focusInFrame("photo");
		driver.waitForScript();
		final DigestUserEdit edit = this;
		return new FileUploadForm(driver) {
			@Override
			public DigestUserEdit uploadAs(String fileName) {
				driver.click(By.id("upload"));
				driver.focusInFrame("photo", "save-as___");
				driver.type(By.id("label"), fileName);
				driver.focusInFrame("photo");
				driver.click(By.xpath("(//button[@type='button'])[2]"));
				return edit;
			}
		};
	}

	public CalliPage save() {
		driver.focusInTopWindow();
		driver.click(By.id("save"));
		return page();
	}

}
