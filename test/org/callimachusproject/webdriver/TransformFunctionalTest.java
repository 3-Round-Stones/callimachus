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

public class TransformFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] transform = new String[] {
			"transform.xsl",
			"<xsl:output method=\"xml\" indent=\"yes\"/>\n" + " \n"
					+ "  <xsl:template match=\"/persons\">\n" + "    <root>\n"
					+ "      <xsl:apply-templates select=\"person\"/>\n"
					+ "    </root>\n" + "  </xsl:template>\n" + " \n"
					+ "  <xsl:template match=\"person\">\n"
					+ "    <name username=\"{@username}\">\n"
					+ "      <xsl:value-of select=\"name\" />\n"
					+ "    </name>\n" + "  </xsl:template>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(TransformFunctionalTest.class);
	}

	public TransformFunctionalTest() {
		super();
	}

	public TransformFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateTransform() {
		String name = transform[0];
		logger.info("Create transform {}", name);
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n"
				+ transform[1] + "\n</xsl:stylesheet>";
		page.openCurrentFolder().openTextCreate("Transform").clear()
				.type(markup).end().saveAs(name);
		logger.info("Delete transform {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
