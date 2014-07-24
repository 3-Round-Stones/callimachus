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

public class PageFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] xhtml = new String[] { "page.xhtml", "Page",
			"<h1>Page</h1>\n" + "<p>Paragraph.</p>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(PageFunctionalTest.class);
	}

	public PageFunctionalTest() {
		super();
	}

	public PageFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreatePage() {
		String name = xhtml[0];
		logger.info("Create page {}", name);
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ "<html\n"
				+ " xmlns=\"http://www.w3.org/1999/xhtml\"\n"
				+ " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
				+ " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
				+ " xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n"
				+ "<head>\n" + "<title>" + xhtml[1] + "</title>\n"
				+ "</head>\n" + "<body>\n" + xhtml[2] + "\n</body>\n</html>";
		page.openCurrentFolder().openTextCreate("Page").setText(markup)
				.saveAs(name);
		logger.info("Delete page {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
