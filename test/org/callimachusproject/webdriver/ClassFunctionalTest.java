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
import org.callimachusproject.webdriver.pages.ClassEdit;
import org.callimachusproject.webdriver.pages.ClassView;
import org.callimachusproject.webdriver.pages.SampleResourceCreate;
import org.callimachusproject.webdriver.pages.SampleResourceEdit;
import org.callimachusproject.webdriver.pages.TextEditor;

public class ClassFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ClassFunctionalTest.class);
	}

	public ClassFunctionalTest() {
		super();
	}

	public ClassFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateClass() {
		String name = "Test";
		String comment = "testing";
		logger.info("Create class {}", name);
		page.openCurrentFolder().openClassCreate().with(name, comment)
				.create().waitUntilTitle(name);
		logger.info("Delete class {}", name);
		page.open(name + "?view").waitUntilTitle(name)
				.openEdit(ClassEdit.class).delete(name);
	}

	public void testCreatableClass() {
		String name = "Test";
		String comment = "testing";
		logger.info("Create class templates for {}", name);
		ClassEdit create = page.openCurrentFolder().openClassCreate();
		create.openCreateTemplate().saveAs("test-create.xhtml");
		create.openViewTemplate().saveAs("test-view.xhtml");
		create.openEditTemplate().saveAs("test-edit.xhtml");
		logger.info("Create class {}", name);
		ClassView cls = create.with(name, comment).create();
		logger.info("Create resource {}", "resource");
		cls.waitUntilTitle(name).createANew(name, SampleResourceCreate.class)
				.with("resource", "A test resource").createAs().back();
		logger.info("Delete resource {}", "resource");
		cls.openIndex(name).openResource("resource")
				.openEdit(SampleResourceEdit.class).delete("resource");
		logger.info("Delete class {}", name);
		page.open(name + "?view").waitUntilTitle(name)
				.openEdit(ClassEdit.class).delete(name);
		logger.info("Delete class templates for {}", name);
		page.open("test-create.xhtml?view").openEdit(TextEditor.class).delete();
		page.open("test-view.xhtml?view").openEdit(TextEditor.class).delete();
		page.open("test-edit.xhtml?view").openEdit(TextEditor.class).delete();
	}

}
