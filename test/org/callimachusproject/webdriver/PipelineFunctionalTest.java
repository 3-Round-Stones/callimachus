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

public class PipelineFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] pipeline = new String[] {
			"pipeline.xpl",
			"<p:input port='schemas' sequence='true'/>\n" + "  <p:xinclude/>\n"
					+ "  <p:validate-with-xml-schema>\n"
					+ "   <p:input port='schema'>\n"
					+ "     <p:pipe step='pipeline.xpl' port='schemas'/>\n"
					+ "   </p:input>\n" + "  </p:validate-with-xml-schema>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(PipelineFunctionalTest.class);
	}

	public PipelineFunctionalTest() {
		super();
	}

	public PipelineFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreatePipeline() {
		String name = pipeline[0];
		logger.info("Create pipeline {}", name);
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ "<p:pipeline version=\"1.0\" name=\"" + name + "\"\n"
				+ " xmlns:p=\"http://www.w3.org/ns/xproc\"\n"
				+ " xmlns:c=\"http://www.w3.org/ns/xproc-step\"\n"
				+ " xmlns:l=\"http://xproc.org/library\">\n" + pipeline[1]
				+ "\n</p:pipeline>";
		page.openCurrentFolder().openTextCreate("Pipeline").clear()
				.type(markup).end().saveAs(name);
		logger.info("Delete pipeline {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
