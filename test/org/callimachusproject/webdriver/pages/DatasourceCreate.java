package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DatasourceCreate extends CalliPage {

	public DatasourceCreate(WebBrowserDriver driver) {
		super(driver);
	}

	public DatasourceCreate with(String label, String comment) {
		browser.type(By.id("label"), label);
		browser.type(By.id("comment"), comment);
		return this;
	}

	public DatasourceView create() {
		browser.click(By.id("create-datasource"));
		return page(DatasourceView.class);
	}

}
