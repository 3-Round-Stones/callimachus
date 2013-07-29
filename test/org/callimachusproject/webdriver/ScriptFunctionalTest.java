package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class ScriptFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] script = new String[] {
			"script.js",
			"function factorial(n) {\n" + "    if (n === 0) {\n"
					+ "        return 1;\n" + "    }\n"
					+ "    return n * factorial(n - 1);\n" + "}" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ScriptFunctionalTest.class);
	}

	public ScriptFunctionalTest() {
		super();
	}

	public ScriptFunctionalTest(BrowserFunctionalTestCase parent) {
		super("", parent);
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

	public void testCreateHtml() {
		String name = script[0];
		logger.info("Create script {}", name);
		page.openCurrentFolder().openSubTextCreate("Script").clear()
				.type(script[1]).end().saveAs(name);
		logger.info("Delete script {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
