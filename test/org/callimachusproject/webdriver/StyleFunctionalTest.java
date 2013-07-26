package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

public class StyleFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] style = new String[] {
			"style.css",
			"body {\n" + "background-color:#d0e4fe;\n" + "}\n" + "h1 {\n"
					+ "color:orange;\n" + "text-align:center;\n" + "}\n"
					+ "p {\n" + "font-family:\"Times New Roman\";\n"
					+ "font-size:20px;\n" + "}" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(StyleFunctionalTest.class);
	}

	public StyleFunctionalTest() {
		super();
	}

	public StyleFunctionalTest(CallimachusDriver driver) {
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
		driver.createStyle(style[0], style[1]);
		driver.deleteStyle(style[0]);
	}

}
