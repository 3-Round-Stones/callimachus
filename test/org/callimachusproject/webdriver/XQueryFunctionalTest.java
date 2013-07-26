package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

public class XQueryFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] xquery = new String[] {
			"xquery.xq",
			"<html><head/><body>\n" + " {\n"
					+ "   for $act in doc('hamlet.xml')//ACT\n"
					+ "   let $speakers := distinct-values($act//SPEAKER)\n"
					+ "   return\n" + "     <div>\n"
					+ "       <h1>{ string($act/TITLE) }</h1>\n"
					+ "       <ul>\n" + "       {\n"
					+ "         for $speaker in $speakers\n"
					+ "         return <li>{ $speaker }</li>\n" + "       }\n"
					+ "       </ul>\n" + "     </div>\n" + " }\n"
					+ " </body></html>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(XQueryFunctionalTest.class);
	}

	public XQueryFunctionalTest() {
		super();
	}

	public XQueryFunctionalTest(CallimachusDriver driver) {
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
		driver.createXQuery(xquery[0], xquery[1]);
		driver.deleteXQuery(xquery[0]);
	}

}
