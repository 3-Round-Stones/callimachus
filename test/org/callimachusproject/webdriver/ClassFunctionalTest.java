package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.ClassEdit;

public class ClassFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ClassFunctionalTest.class);
	}

	public ClassFunctionalTest() {
		super();
	}

	public ClassFunctionalTest(BrowserFunctionalTestCase parent) {
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

	public void testCreateClass() {
		String name = "Test";
		String comment = "testing";
		logger.info("Create class {}", name);
		page.openCurrentFolder().openClassCreate().with(name, comment)
				.create().waitUntilTitle(name);
		logger.info("Delete class {}", name);
		page.open(name + "?view").waitUntilTitle(name)
				.openEdit(ClassEdit.class).delete(name);
	}

}
