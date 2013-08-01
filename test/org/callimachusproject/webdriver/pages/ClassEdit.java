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

	public ClassView create() {
		driver.submit(By.id("create"));
		return page(ClassView.class);
	}

	public CalliPage delete(String conceptLabel) {
		driver.click(By.id("delete"));
		driver.confirm("Are you sure you want to delete " + conceptLabel);
		return page();
	}

	public TextEditor openCreateTemplate() {
		driver.click(By.cssSelector("#create label.control-label a"));
		driver.waitForScript();
		driver.focusInFrame("create-template");
		driver.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("create-template", driver) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				driver.focusInTopWindow();
				return edit;
			}
		};
	}

	public TextEditor openViewTemplate() {
		driver.click(By.cssSelector("#view label.control-label a"));
		driver.waitForScript();
		driver.focusInFrame("view-template");
		driver.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("view-template", driver) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				driver.focusInTopWindow();
				return edit;
			}
		};
	}

	public TextEditor openEditTemplate() {
		driver.click(By.cssSelector("#edit label.control-label a"));
		driver.waitForScript();
		driver.focusInFrame("edit-template");
		driver.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("edit-template", driver) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				driver.focusInTopWindow();
				return edit;
			}
		};
	}

}
