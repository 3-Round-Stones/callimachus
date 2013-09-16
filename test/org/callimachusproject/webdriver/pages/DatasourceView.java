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
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + label);
		return page();
	}

	public DatasourceView query(String query) {
		browser.type(By.id("sparql"), query);
		return this;
	}

	public SparqlResult evaluate() {
		browser.click(By.id("evaluate"));
		return page(SparqlResult.class);
	}

	public DatasourceView execute() {
		browser.click(By.id("execute"));
		return page(DatasourceView.class);
	}

}
