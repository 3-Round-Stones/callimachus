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
import org.callimachusproject.webdriver.pages.DomainEdit;

public class DomainFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(DomainFunctionalTest.class);
	}

	public DomainFunctionalTest() {
		super();
	}

	public DomainFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateDomain() {
		String name = "test";
		String comment = "testing";
		logger.info("Create domain {}", name);
		page.openCurrentFolder().openDomainCreate().with(name, comment)
				.create().waitUntilTitle(name);
		logger.info("Delete domain {}", name);
		page.open(name + "?view").waitUntilTitle(name)
				.openEdit(DomainEdit.class).delete(name);
	}

}
