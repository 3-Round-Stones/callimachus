package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class PipelineFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] pipeline = new String[] {
			"pipeline.xpl",
			"<p:input port='schemas' sequence='true'/>\n" + "  <p:xinclude/>\n"
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

	public PipelineFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreatePipeline() {
		String name = pipeline[0];
		logger.info("Create pipeline {}", name);
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ "<p:pipeline version=\"1.0\" name=\"" + name + "\"\n"
				+ " xmlns:p=\"http://www.w3.org/ns/xproc\"\n"
				+ " xmlns:c=\"http://www.w3.org/ns/xproc-step\"\n"
				+ " xmlns:l=\"http://xproc.org/library\">\n" + pipeline[1]
				+ "\n</p:pipeline>";
		page.openCurrentFolder().openTextCreate("Pipeline").clear()
				.type(markup).end().saveAs(name);
		logger.info("Delete pipeline {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
