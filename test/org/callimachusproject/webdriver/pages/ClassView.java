package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ClassView extends CalliPage {

	public ClassView(WebBrowserDriver driver) {
		super(driver);
	}

	@Override
	public ClassView waitUntilTitle(String title) {
		super.waitUntilTitle(title);
		return this;
	}

	public <P> P createANew(String label, Class<P> page) {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("i.icon-cog"));
		browser.click(By.linkText("Create a new " + label));
		return page(page);
	}

	public ClassIndex openIndex(String label) {
		browser.focusInTopWindow();
		browser.click(By.cssSelector("i.icon-cog"));
		browser.click(By.linkText(label + " resources"));
		return page(ClassIndex.class);
	}

}
