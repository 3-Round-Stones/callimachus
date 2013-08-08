package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DescribePage extends CalliPage {

	public DescribePage(WebBrowserDriver driver) {
		super(driver);
	}

	public DescribePage describe(String curie) {
		driver.click(By.xpath("//li[span/text() = '" + curie + "']/a[@class='describe']"));
		return page(DescribePage.class);
	}

}
