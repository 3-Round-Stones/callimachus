/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
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
import org.callimachusproject.webdriver.pages.ConceptEdit;
import org.openqa.selenium.By;

public class ConceptFunctionalTest extends BrowserFunctionalTestCase {
	public static String[] stars = { "stars", "Stars",
							"Surely the stars are images of love.",
							"The stars are golden fruit upon a tree, All out of reach.",
							"These blessed candles of the night."};
	public static String[] sun = { "sun", "Sun", "The lamp of day",
			"The sun is the king of torches." };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ConceptFunctionalTest.class);
	}

	public ConceptFunctionalTest() {
		super();
	}

	public ConceptFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateConcept() {
		String conceptName = stars[0];
		String conceptLabel = stars[1];
		logger.info("Create concept {}", conceptName);
		page.openCurrentFolder().openConceptCreate()
				.with(conceptName, conceptLabel, stars[2], stars[3])
				.create().waitUntilTitle(conceptLabel);
		page.searchFor(conceptLabel).openResult(conceptLabel);
		logger.info("Delete concept {}", conceptName);
		page.open(conceptName + "?view").waitUntilTitle(conceptLabel)
				.openEdit(ConceptEdit.class).delete(conceptLabel);
	}

	public void testConceptHistory() {
		String conceptName = stars[0];
		String conceptLabel = stars[1];
		String def = stars[2];
		String example = stars[3];
		String altDef = stars[4];
		logger.info("Create concept {}", conceptName);
		page.openCurrentFolder().openConceptCreate()
				.with(conceptName, conceptLabel, def, example).create()
				.waitUntilTitle(conceptLabel);
		logger.info("Modify concept {}", conceptName);
		page.openEdit(ConceptEdit.class).definition(altDef).save()
				.waitUntilTitle(conceptLabel).openHistory().openLastModified()
				.waitUntilText(By.cssSelector(".container .literal"), def)
				.waitUntilText(By.cssSelector(".container .literal"), altDef)
				.back();
		logger.info("Delete concept {}", conceptName);
		page.openEdit(ConceptEdit.class).delete(conceptLabel);
	}

	public void testConceptDiscussion() {
		String conceptName = stars[0];
		String conceptLabel = stars[1];
		String def = stars[2];
		String example = stars[3];
		String altDef = stars[4];
		logger.info("Create concept {}", conceptName);
		page.openCurrentFolder().openConceptCreate()
				.with(conceptName, conceptLabel, def, example).create()
				.waitUntilTitle(conceptLabel);
		logger.info("Discuss concept {}", conceptName);
		page.openDiscussion().with(altDef).post()
				.waitUntilText(By.cssSelector(".panel-body"), altDef);
		logger.info("Delete concept {}", conceptName);
		page.openEdit(ConceptEdit.class).delete(conceptLabel);
	}

	public void testNarrowConcept() {
		String conceptName = stars[0];
		String conceptLabel = stars[1];
		String def = stars[2];
		String example = stars[3];
		logger.info("Create concept {}", conceptName);
		page.openCurrentFolder().openConceptCreate()
				.with(conceptName, conceptLabel, def, example).create()
				.waitUntilTitle(conceptLabel);
		logger.info("Create concept {}", sun[0]);
		ConceptEdit edit = page.openEdit(ConceptEdit.class);
		edit.openNarrowDialogue().with(sun[0], sun[1], sun[2], sun[3]).create();
		edit.save().openRelatedChanges().openResource(sun[1])
				.waitUntilTitle(sun[1]).openWhatLinksHere()
				.openResult(conceptLabel);
		logger.info("Delete concept {}", conceptName);
		page.openEdit(ConceptEdit.class).delete(conceptLabel);
		logger.info("Delete concept {}", sun[0]);
		page.open(sun[0] + "?view").waitUntilTitle(sun[1])
				.openEdit(ConceptEdit.class).delete(sun[1]);
	}

}
