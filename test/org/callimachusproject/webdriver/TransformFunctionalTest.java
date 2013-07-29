package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class TransformFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] transform = new String[] {
			"transform.xsl",
			"<xsl:output method=\"xml\" indent=\"yes\"/>\n" + " \n"
					+ "  <xsl:template match=\"/persons\">\n" + "    <root>\n"
					+ "      <xsl:apply-templates select=\"person\"/>\n"
					+ "    </root>\n" + "  </xsl:template>\n" + " \n"
					+ "  <xsl:template match=\"person\">\n"
					+ "    <name username=\"{@username}\">\n"
					+ "      <xsl:value-of select=\"name\" />\n"
					+ "    </name>\n" + "  </xsl:template>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(TransformFunctionalTest.class);
	}

	public TransformFunctionalTest() {
		super();
	}

	public TransformFunctionalTest(BrowserFunctionalTestCase parent) {
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

	public void testCreateTransform() {
		String name = transform[0];
		logger.info("Create transform {}", name);
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n"
				+ transform[1] + "\n</xsl:stylesheet>";
		page.openCurrentFolder().openTextCreate("Transform").clear()
				.type(markup).end().saveAs(name);
		logger.info("Delete transform {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
