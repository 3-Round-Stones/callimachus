package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class ClassEdit extends CalliPage {

	public ClassEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public ClassEdit with(String label, String comment) {
		browser.type(By.id("label"), label);
		browser.type(By.id("comment"), comment);
		return this;
	}

	public ClassView create() {
		browser.submit(By.id("create"));
		return page(ClassView.class);
	}

	public CalliPage delete(String conceptLabel) {
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + conceptLabel);
		return page();
	}

	public TextEditor openCreateTemplate() {
		browser.click(By.cssSelector("#create label a"));
		browser.waitForScript();
		browser.focusInFrame("template-for-creating");
		browser.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("template-for-creating", browser) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				browser.focusInTopWindow();
				return edit;
			}
		};
	}

	public TextEditor openViewTemplate() {
		browser.click(By.cssSelector("#view label a"));
		browser.waitForScript();
		browser.focusInFrame("template-for-viewing");
		browser.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("template-for-viewing", browser) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				browser.focusInTopWindow();
				return edit;
			}
		};
	}

	public TextEditor openEditTemplate() {
		browser.click(By.cssSelector("#edit label a"));
		browser.waitForScript();
		browser.focusInFrame("template-for-editing");
		browser.waitForScript();
		final ClassEdit edit = this;
		return new TextEditor("template-for-editing", browser) {
			@Override
			public ClassEdit saveAs(String name) {
				super.saveAs(name);
				browser.focusInTopWindow();
				return edit;
			}
		};
	}

}
