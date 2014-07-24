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

public class XQueryFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] xquery = new String[] {
			"xquery.xq",
			"<html><head/><body>\n" + " {\n"
					+ "   for $act in doc('hamlet.xml')//ACT\n"
					+ "   let $speakers := distinct-values($act//SPEAKER)\n"
					+ "   return\n" + "     <div>\n"
					+ "       <h1>{ string($act/TITLE) }</h1>\n"
					+ "       <ul>\n" + "       {\n"
					+ "         for $speaker in $speakers\n"
					+ "         return <li>{ $speaker }</li>\n" + "       }\n"
					+ "       </ul>\n" + "     </div>\n" + " }\n"
					+ " </body></html>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(XQueryFunctionalTest.class);
	}

	public XQueryFunctionalTest() {
		super();
	}

	public XQueryFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateXQuery() {
		String name = xquery[0];
		logger.info("Create xquery {}", name);
		page.openCurrentFolder().openTextCreate("XQuery").setText(xquery[1])
				.saveAs(name);
		logger.info("Delete xquery {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
