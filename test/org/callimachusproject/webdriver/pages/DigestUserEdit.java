package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DigestUserEdit extends CalliPage {

	public DigestUserEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public FileUploadForm openPhotoUpload() {
		browser.click(By.cssSelector("#photo label a"));
		browser.waitForScript();
		browser.focusInFrame("photo");
		browser.waitForScript();
		final DigestUserEdit edit = this;
		return new FileUploadForm(browser) {
			@Override
			public DigestUserEdit uploadAs(String fileName) {
				browser.click(By.id("upload"));
				browser.focusInFrame("photo", "save-as___");
				browser.type(By.id("label"), fileName);
				browser.focusInFrame("photo");
				browser.click(By.xpath("//div[@role='dialog']//button[text()='Save']"));
				browser.waitForFrameToClose("photo");
				return edit;
			}
		};
	}

	public CalliPage save() {
		browser.focusInTopWindow();
		browser.click(By.id("save"));
		return page();
	}

	public FolderView delete(String label) {
		browser.focusInTopWindow();
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + label);
		return page(FolderView.class);
	}

}
