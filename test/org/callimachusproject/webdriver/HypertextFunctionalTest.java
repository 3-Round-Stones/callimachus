package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

public class HypertextFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] html = new String[] {
			"hypertext.html",
			"Hypertext",
			"<h1>Heading</h1>\n" +
			"<p>Paragraph.</p>"};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(HypertextFunctionalTest.class);
	}

	public HypertextFunctionalTest() {
		super();
	}

	public HypertextFunctionalTest(CallimachusDriver driver) {
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
		driver.createHypertext(html[0], html[1], html[2]);
		driver.deleteHypertext(html[0], html[1]);
	}

}
