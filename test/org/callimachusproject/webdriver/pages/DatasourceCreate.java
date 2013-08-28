package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DatasourceCreate extends CalliPage {

	public DatasourceCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public DatasourceCreate with(String label, String comment) {
		driver.type(By.id("label"), label);
		driver.type(By.id("comment"), comment);
		return this;
	}

	public DatasourceView create() {
		driver.click(By.id("create-datasource"));
		return page(DatasourceView.class);
	}

}
