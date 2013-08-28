package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.DatasourceEdit;
import org.callimachusproject.webdriver.pages.DatasourceView;

public class DatasourceFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(DatasourceFunctionalTest.class);
	}

	public DatasourceFunctionalTest() {
		super();
	}

	public DatasourceFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateDatasource() {
		String datasourceName = "data";
		String datasourceLabel = "Data";
		logger.info("Create datasource {}", datasourceName);
		DatasourceView view = page.openCurrentFolder().openDatasourceCreate()
				.with(datasourceLabel, "Test data").create()
				.waitUntilTitle(datasourceLabel);
		view.query("ASK { ?s ?p ?o }").evaluate().waitUntilResult("false")
				.back();
		view.query("INSERT DATA { <> a rdfs:Resource }").execute();
		view.query("ASK { ?s ?p ?o }").evaluate().waitUntilResult("true")
				.back();
		logger.info("Delete datasource {}", datasourceName);
		page.open(datasourceName + "?view").waitUntilTitle(datasourceLabel)
				.openEdit(DatasourceEdit.class).delete(datasourceLabel);
	}

}
