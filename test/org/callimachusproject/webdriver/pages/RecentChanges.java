package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class RecentChanges extends CalliPage {

	public RecentChanges(WebBrowserDriver driver) {
		super(driver);
	}

	public RecentChanges openResource(String label) {
		browser.click(By.linkText(label));
		return this;
	}

	public ChangeView openChange(String label) {
		browser.click(By.xpath("//li[a//text() = \"" + label + "\"]/a[time]"));
		return page(ChangeView.class);
	}

}
