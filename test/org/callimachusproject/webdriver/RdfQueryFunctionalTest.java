package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class RdfQueryFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] query = new String[] {
			"query.rq",
			"SELECT ?s { ?s a ?cls } LIMIT 1" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(RdfQueryFunctionalTest.class);
	}

	public RdfQueryFunctionalTest() {
		super();
	}

	public RdfQueryFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateRdfQuery() {
		String name = query[0];
		logger.info("Create rdf query {}", name);
		page.openCurrentFolder().openTextCreate("RdfQuery").clear()
				.type(query[1]).end().saveAs(name);
		logger.info("Delete rdf query {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
