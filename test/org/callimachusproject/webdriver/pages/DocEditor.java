package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

public class DocEditor extends CalliPage {

	public DocEditor(WebBrowserDriver driver) {
		super(driver);
	}

	public DocEditor clear() {
		browser.focusInFrameIndex(0, 0);
		browser.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		browser.sendKeys(Keys.DELETE);
		CharSequence[] keys = new CharSequence[32];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Keys.BACK_SPACE;
		}
		browser.sendKeys(keys);
		return this;
	}

	public DocEditor type(String text) {
		browser.focusInFrameIndex(0, 0);
		browser.sendKeys(text);
		return this;
	}

	public DocEditor heading1() {
		browser.focusInFrameIndex(0);
		browser.click(By.cssSelector(".cke_combo__format a.cke_combo_button"));
		browser.focusInFrameIndex(0, 1);
		browser.click(By.partialLinkText("Heading 1"));
		return this;
	}

	public CalliPage saveAs(String articleName) {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("button.btn-success"));
		browser.focusInFrame("save-as___");
		browser.type(By.id("label"), articleName);
		browser.focusInTopWindow();
		browser.click(By.xpath("//div[@role='dialog']//button[1]"));
		return page();
	}

	public CalliPage delete() {
		browser.click(By.cssSelector("button.btn.btn-danger"));
		browser.confirm("Are you sure you want to delete");
		return page();
	}

}
