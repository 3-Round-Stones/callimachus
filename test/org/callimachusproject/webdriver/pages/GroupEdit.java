package org.callimachusproject.webdriver.pages;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class GroupEdit extends CalliPage {

	public GroupEdit(WebBrowserDriver driver) {
		super(driver);
	}

	public CalliPage delete(String label) {
		driver.click(By.id("delete"));
		driver.confirm("Are you sure you want to delete " + label);
		return page();
	}

	public CalliPage save() {
		driver.click(By.id("save"));
		return page();
	}

	public InviteUser openInviteUser() {
		driver.click(By.cssSelector("#members label.control-label a"));
		driver.waitForScript();
		driver.focusInFrame("members");
		driver.waitForScript();
		final GroupEdit edit = this;
		return new InviteUser(driver) {
			@Override
			public GroupEdit invite() {
				driver.click(By.id("invite"));
				driver.waitForFrameToClose("members");
				return edit;
			}
		};
	}

}
