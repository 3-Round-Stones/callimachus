package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class DatasourceView extends CalliPage {

	public DatasourceView(WebBrowserDriver driver) {
		super(driver);
	}

	@Override
	public DatasourceView waitUntilTitle(String title) {
		super.waitUntilTitle(title);
		return this;
	}

	public CalliPage delete(String label) {
		driver.click(By.id("delete"));
		driver.confirm("Are you sure you want to delete " + label);
		return page();
	}

	public DatasourceView query(String query) {
		driver.type(By.id("sparql"), query);
		return this;
	}

	public SparqlResult evaluate() {
		driver.click(By.id("evaluate"));
		return page(SparqlResult.class);
	}

	public DatasourceView execute() {
		driver.click(By.id("execute"));
		return page(DatasourceView.class);
	}

}
