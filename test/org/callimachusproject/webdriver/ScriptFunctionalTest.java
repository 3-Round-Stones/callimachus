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

public class ScriptFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] script = new String[] {
			"script.js",
			"function factorial(n) {\n" + "    if (n === 0) {\n"
					+ "        return 1;\n" + "    }\n"
					+ "    return n * factorial(n - 1);\n" + "}" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ScriptFunctionalTest.class);
	}

	public ScriptFunctionalTest() {
		super();
	}

	public ScriptFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateScript() {
		String name = script[0];
		logger.info("Create script {}", name);
		page.openCurrentFolder().openTextCreate("Script").setText(script[1])
				.saveAs(name);
		logger.info("Delete script {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
