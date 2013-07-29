package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class HypertextFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] html = new String[] { "hypertext.html",
			"Hypertext", "<h1>Heading</h1>\n" + "<p>Paragraph.</p>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(HypertextFunctionalTest.class);
	}

	public HypertextFunctionalTest() {
		super();
	}

	public HypertextFunctionalTest(BrowserFunctionalTestCase parent) {
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
		String name = html[0];
		logger.info("Create hypertext {}", name);
		String markup = "<!DOCTYPE html>\n" + "<html>\n" + "<title>" + html[1]
				+ "</title>\n" + "<body>\n" + html[2] + "\n</body>\n</html>";
		page.openCurrentFolder().openTextCreate("Hypertext File").clear()
				.type(markup).end().saveAs(name);
		logger.info("Delete hypertext {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
