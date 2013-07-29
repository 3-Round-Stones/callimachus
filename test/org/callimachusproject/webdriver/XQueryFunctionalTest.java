package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

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

	public XQueryFunctionalTest(BrowserFunctionalTestCase parent) {
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

	public void testCreateXQuery() {
		String name = xquery[0];
		logger.info("Create xquery {}", name);
		page.openCurrentFolder().openTextCreate("XQuery").clear()
				.type(xquery[1]).end().saveAs(name);
		logger.info("Delete xquery {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
