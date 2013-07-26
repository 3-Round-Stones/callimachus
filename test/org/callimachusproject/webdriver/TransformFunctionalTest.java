package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

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

	public TransformFunctionalTest(CallimachusDriver driver) {
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
		driver.createTransform(transform[0], transform[1]);
		driver.deleteTransform(transform[0]);
	}

}
