package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

public class DocEditor extends CalliPage {

	public DocEditor(WebBrowserDriver driver) {
		super(driver);
	}

	public DocEditor clear() {
		driver.focusInFrame(0, 0);
		driver.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		driver.sendKeys(Keys.DELETE);
		CharSequence[] keys = new CharSequence[32];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = Keys.BACK_SPACE;
		}
		driver.sendKeys(keys);
		return this;
	}

	public DocEditor type(String text) {
		driver.focusInFrame(0, 0);
		driver.sendKeys(text);
		return this;
	}

	public DocEditor heading1() {
		driver.focusInFrame(0);
		driver.click(By.cssSelector(".cke_combo__format a.cke_combo_button"));
		driver.focusInFrame(0, 1);
		driver.click(By.partialLinkText("Heading 1"));
		return this;
	}

	public CalliPage saveAs(String articleName) {
		driver.focusInTopWindow();
		driver.click(By.id("create-article"));
		driver.focusInFrame("save-as___");
		driver.type(By.id("label"), articleName);
		driver.focusInTopWindow();
		driver.click(By.xpath("(//button[@type='button'])[2]"));
		return page();
	}

	public CalliPage delete() {
		driver.click(By.cssSelector("button.btn.btn-danger"));
		driver.confirm("Are you sure you want to delete");
		return page();
	}

}
