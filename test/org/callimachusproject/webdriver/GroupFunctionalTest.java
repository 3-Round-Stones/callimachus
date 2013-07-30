package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.GroupEdit;

public class GroupFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(GroupFunctionalTest.class);
	}

	public GroupFunctionalTest() {
		super();
	}

	public GroupFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		String username = getUsername();
		logger.info("Login {}", username);
		page.openLogin().with(username, getPassword()).login();
	}

	@Override
	public void tearDown() throws Exception {
		logger.info("Logout");
		page.logout();
		super.tearDown();
	}

	public void testCreateGroup() {
		String name = "test";
		String comment = "testing";
		logger.info("Create group {}", name);
		page.openCurrentFolder().openGroupCreate().with(name, comment)
				.create().waitUntilTitle(name);
		logger.info("Delete group {}", name);
		page.open(name + "?view").waitUntilTitle(name)
				.openEdit(GroupEdit.class).delete(name);
	}

}
