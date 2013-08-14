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
		driver.focusInSubFrame(topFrameName, 0);
		// shift/control does not appear to work in IE
		CharSequence[] keys = new CharSequence[1024];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Keys.BACK_SPACE;
		}
		driver.sendKeys(By.tagName("textarea"), keys);
		return this;
	}

	public TextEditor type(String text) {
		driver.focusInSubFrame(topFrameName, 0);
		driver.sendKeys(By.tagName("textarea"), text);
		return this;
	}

	public TextEditor end() {
		driver.focusInSubFrame(topFrameName, 0);
		// shift/control does not appear to work in IE
		CharSequence[] keys = new CharSequence[1024];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Keys.DELETE;
		}
		driver.sendKeys(By.tagName("textarea"), keys);
		return this;
	}

	public CalliPage saveAs(String name) {
		driver.focusInFrame(topFrameName);
		driver.click(By.cssSelector("button.btn-success"));
		driver.focusInFrame(topFrameName, "save-as___");
		driver.type(By.id("label"), name);
		driver.focusInFrame(topFrameName);
		driver.click(By.xpath("(//button[@type='button'])[2]"));
		driver.waitForFrameToClose(topFrameName);
		return page();
	}

	public CalliPage delete() {
		driver.click(By.cssSelector("button.btn.btn-danger"));
		driver.confirm("Are you sure you want to delete");
		driver.waitForScript();
		return page();
	}

}
