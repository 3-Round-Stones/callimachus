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

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ConceptFunctionalTest.class);
	}

	public ConceptFunctionalTest() {
		super();
	}

	public ConceptFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		String username = getUsername();
		logger.info("Login {}", username);
		page.openLogin().with(username, getPassword()).login();
	}

	@Override
	public void tearDown() throws Exception {
		logger.info("Logout");
		page.logout();
		super.tearDown();
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
				.waitUntilText(By.cssSelector(".tab-content .literal"), def)
				.waitUntilText(By.cssSelector(".tab-content .literal"), altDef)
				.back();
		page.openRecentChanges().openChange(conceptLabel)
				.waitUntilText(By.cssSelector(".tab-content .literal"), def)
				.waitUntilText(By.cssSelector(".tab-content .literal"), altDef)
				.back().back();
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
				.waitUntilText(By.cssSelector(".wiki"), altDef);
		logger.info("Delete concept {}", conceptName);
		page.openEdit(ConceptEdit.class).delete(conceptLabel);
	}

}
