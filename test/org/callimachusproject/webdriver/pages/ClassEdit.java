package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ClassEdit extends CalliPage {

	public ClassEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public ClassEdit with(String label, String comment) {
		driver.type(By.id("label"), label);
		driver.type(By.id("comment"), comment);
		return this;
	}

	public CalliPage create() {
		driver.submit(By.id("create"));
		return page();
	}

	public CalliPage delete(String conceptLabel) {
		driver.click(By.id("delete"));
		driver.confirm("Are you sure you want to delete " + conceptLabel);
		return page();
	}

}
