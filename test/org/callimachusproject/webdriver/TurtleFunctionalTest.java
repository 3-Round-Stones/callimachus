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
import org.callimachusproject.webdriver.pages.TextEditor;

public class TurtleFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] turtle = new String[] {
			"test.ttl",
			"<> a <Test> ." };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(TurtleFunctionalTest.class);
	}

	public TurtleFunctionalTest() {
		super();
	}

	public TurtleFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateTurtle() {
		String name = turtle[0];
		logger.info("Create turtle {}", name);
		page.openCurrentFolder().openTextCreate("RdfTurtle").clear()
				.type(turtle[1]).end().saveAs(name);
		logger.info("Delete turtle {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
