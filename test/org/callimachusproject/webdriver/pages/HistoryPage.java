package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class HistoryPage extends CalliPage {

	public HistoryPage(WebBrowserDriver driver) {
		super(driver);
	}

	public ChangeView openLastModified() {
		browser.click(By.xpath("//a[time]"));
		return page(ChangeView.class);
	}

}
