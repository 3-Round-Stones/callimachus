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

public class FolderFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(FolderFunctionalTest.class);
	}

	public FolderFunctionalTest() {
		super();
	}

	public FolderFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testHomeFolderView() {
		page.openHomeFolder().assertLayout();
	}

	public void testCreateFolder() {
		String folderName = "Bob's%20R&D+Folder";
		logger.info("Create folder {}", folderName);
		page.openCurrentFolder().openFolderCreate().with(folderName).create()
				.waitUntilFolderOpen(folderName);
		logger.info("Delete folder {}", folderName);
		page.openCurrentFolder().waitUntilFolderOpen(folderName).openEdit()
				.waitUntilTitle(folderName).delete();
	}

}
