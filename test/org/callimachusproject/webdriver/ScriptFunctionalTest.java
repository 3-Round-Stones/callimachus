package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

public class ScriptFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] script = new String[] {
			"script.js",
			"function factorial(n) {\n" +
			"    if (n === 0) {\n" +
			"        return 1;\n" +
			"    }\n" +
			"    return n * factorial(n - 1);\n" +
			"}"};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ScriptFunctionalTest.class);
	}

	public ScriptFunctionalTest() {
		super();
	}

	public ScriptFunctionalTest(CallimachusDriver driver) {
		super("", driver);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		driver.login(getUsername(), getPassword());
	}

	@Override
	public void tearDown() throws Exception {
		driver.logout();
		super.tearDown();
	}

	public void testCreateHtml() {
		driver.createScript(script[0], script[1]);
		driver.deleteScript(script[0]);
	}

}
