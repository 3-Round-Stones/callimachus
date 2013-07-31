package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class TurtleFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] turtle = new String[] {
			"test.ttl",
			"<> a <Test> ." };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(TurtleFunctionalTest.class);
	}

	public TurtleFunctionalTest() {
		super();
	}

	public TurtleFunctionalTest(BrowserFunctionalTestCase parent) {
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

	public void testCreateTurtle() {
		String name = turtle[0];
		logger.info("Create turtle {}", name);
		page.openCurrentFolder().openTextCreate("GraphDocument").clear()
				.type(turtle[1]).end().saveAs(name);
		logger.info("Delete turtle {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
