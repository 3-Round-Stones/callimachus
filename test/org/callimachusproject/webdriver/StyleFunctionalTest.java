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

public class StyleFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] style = new String[] {
			"style.css",
			"body {\n" + "background-color:#d0e4fe;\n" + "}\n" + "h1 {\n"
					+ "color:orange;\n" + "text-align:center;\n" + "}\n"
					+ "p {\n" + "font-family:\"Times New Roman\";\n"
					+ "font-size:20px;\n" + "}" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(StyleFunctionalTest.class);
	}

	public StyleFunctionalTest() {
		super();
	}

	public StyleFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateStyle() {
		String name = style[0];
		logger.info("Create style {}", name);
		page.openCurrentFolder().openTextCreate("StyleSheet").clear()
				.type(style[1]).end().saveAs(name);
		logger.info("Delete style {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
