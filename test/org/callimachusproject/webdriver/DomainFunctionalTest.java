package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.DomainEdit;

public class DomainFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(DomainFunctionalTest.class);
	}

	public DomainFunctionalTest() {
		super();
	}

	public DomainFunctionalTest(BrowserFunctionalTestCase parent) {
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

	public void testCreateDomain() {
		String name = "test";
		String comment = "testing";
		logger.info("Create domain {}", name);
		page.openCurrentFolder().openDomainCreate().with(name, comment)
				.create().waitUntilTitle(name);
		logger.info("Delete domain {}", name);
		page.open(name + "?view").waitUntilTitle(name)
				.openEdit(DomainEdit.class).delete(name);
	}

}
