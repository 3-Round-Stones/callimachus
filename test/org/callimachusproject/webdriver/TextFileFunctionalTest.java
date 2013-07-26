package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

public class TextFileFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] text = new String[] {
			"text.txt",
			"plain text file" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(TextFileFunctionalTest.class);
	}

	public TextFileFunctionalTest() {
		super();
	}

	public TextFileFunctionalTest(CallimachusDriver driver) {
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
		driver.createTextFile(text[0], text[1]);
		driver.deleteTextFile(text[0]);
	}

}
