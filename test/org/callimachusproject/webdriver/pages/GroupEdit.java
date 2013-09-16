package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class GroupEdit extends CalliPage {

	public GroupEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		browser.click(By.id("delete"));
		browser.confirm("Are you sure you want to delete " + label);
		return page();
	}

	public CalliPage save() {
		browser.click(By.id("save"));
		return page();
	}

	public InviteUser openInviteUser() {
		browser.click(By.cssSelector("#members label.control-label a"));
		browser.waitForScript();
		browser.focusInFrame("members");
		browser.waitForScript();
		final GroupEdit edit = this;
		return new InviteUser(browser) {
			@Override
			public GroupEdit invite() {
				browser.click(By.id("invite"));
				browser.waitForFrameToClose("members");
				return edit;
			}
		};
	}

}
