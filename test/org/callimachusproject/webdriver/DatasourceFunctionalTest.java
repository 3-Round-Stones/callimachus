/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
		view.waitUntilTitle(datasourceLabel).openEdit(DatasourceEdit.class)
				.delete(datasourceLabel);
	}

}
