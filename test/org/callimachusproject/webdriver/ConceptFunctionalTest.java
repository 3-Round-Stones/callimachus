package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.ConceptEdit;
import org.openqa.selenium.By;

public class ConceptFunctionalTest extends BrowserFunctionalTestCase {
	public static Map<String, String[]> concepts = new LinkedHashMap<String, String[]>() {
		private static final long serialVersionUID = -5837562534292090399L;
		{
			put("stars",
					new String[] { "stars", "Stars",
							"Surely the stars are images of love.",
							"The stars are golden fruit upon a tree, All out of reach.",
							"These blessed candles of the night."});
		}
	};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ConceptFunctionalTest.class,
				concepts.keySet());
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

	public void testCreateConcept(String variation) {
		String[] concept = concepts.get(variation);
		String conceptName = concept[0];
		String conceptLabel = concept[1];
		logger.info("Create concept {}", conceptName);
		page.openCurrentFolder().openConceptCreate()
				.with(conceptName, conceptLabel, concept[2], concept[3])
				.create().waitUntilTitle(conceptLabel);
		logger.info("Delete concept {}", conceptName);
		page.open(conceptName + "?view").waitUntilTitle(conceptLabel)
				.openEdit(ConceptEdit.class).delete(conceptLabel);
	}

	public void testConceptHistory(String variation) {
		String[] concept = concepts.get(variation);
		String conceptName = concept[0];
		String conceptLabel = concept[1];
		String def = concept[2];
		String example = concept[3];
		String altDef = concept[4];
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
		logger.info("Delete concept {}", conceptName);
		page.openEdit(ConceptEdit.class).delete(conceptLabel);
	}

	public void testConceptDiscussion(String variation) {
		String[] concept = concepts.get(variation);
		String conceptName = concept[0];
		String conceptLabel = concept[1];
		String def = concept[2];
		String example = concept[3];
		String altDef = concept[4];
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
