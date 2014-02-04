package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

public class TextEditor extends CalliPage {
	private String topFrameName;

	public TextEditor(WebBrowserDriver driver) {
		super(driver);
	}

	public TextEditor(String topFrameName, WebBrowserDriver driver) {
		super(driver);
		this.topFrameName = topFrameName;
	}

	public TextEditor clear() {
		browser.focusInFrame(topFrameName, "editor-iframe");
		// shift/control does not appear to work in IE
		CharSequence[] keys = new CharSequence[1024];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Keys.BACK_SPACE;
		}
		browser.sendKeys(By.tagName("textarea"), keys);
		return this;
	}

	public TextEditor type(String text) {
		browser.focusInFrame(topFrameName, "editor-iframe");
		browser.sendKeys(By.tagName("textarea"), text);
		return this;
	}

	public TextEditor end() {
		browser.focusInFrame(topFrameName, "editor-iframe");
		// shift/control does not appear to work in IE
		CharSequence[] keys = new CharSequence[1024];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Keys.DELETE;
		}
		browser.sendKeys(By.tagName("textarea"), keys);
		return this;
	}

	public CalliPage saveAs(String name) {
		browser.focusInFrame(topFrameName);
		browser.click(By.cssSelector("button.btn-success"));
		browser.focusInFrame(topFrameName, "save-as___");
		browser.type(By.id("label"), name);
		browser.focusInFrame(topFrameName);
		browser.click(By.xpath("//div[@role='dialog']//button[text()='Save']"));
		browser.waitForFrameToClose(topFrameName);
		return page();
	}

	public CalliPage delete() {
		browser.click(By.cssSelector("button.btn.btn-danger"));
		browser.confirm("Are you sure you want to delete");
		browser.waitForScript();
		return page();
	}

}
