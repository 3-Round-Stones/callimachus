package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class RecentChanges extends CalliPage {

	public RecentChanges(WebBrowserDriver driver) {
		super(driver);
	}

	public ChangeView openChange(String label) {
		driver.click(By.xpath("//li[a//text() = \"" + label + "\"]/a[time]"));
		return page(ChangeView.class);
	}

}
