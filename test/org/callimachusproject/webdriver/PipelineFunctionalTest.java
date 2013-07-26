package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

public class PipelineFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] pipeline = new String[] {
			"pipeline.xpl",
			"<p:input port='schemas' sequence='true'/>\n"
					+ "  <p:xinclude/>\n"
					+ "  <p:validate-with-xml-schema>\n"
					+ "   <p:input port='schema'>\n"
					+ "     <p:pipe step='pipeline.xpl' port='schemas'/>\n"
					+ "   </p:input>\n" + "  </p:validate-with-xml-schema>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(PipelineFunctionalTest.class);
	}

	public PipelineFunctionalTest() {
		super();
	}

	public PipelineFunctionalTest(CallimachusDriver driver) {
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
		driver.createPipeline(pipeline[0], pipeline[1]);
		driver.deletePipeline(pipeline[0]);
	}

}
