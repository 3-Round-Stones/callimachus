package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.ConceptEdit;

public class ConceptFunctionalTest extends BrowserFunctionalTestCase {
	public static Map<String, String[]> concepts = new LinkedHashMap<String, String[]>() {
		private static final long serialVersionUID = -5837562534292090399L;
		{
			put("sun", new String[] { "sun", "Sun", "The lamp of day",
					"The sun is the king of torches." });
			put("moon",
					new String[] {
							"moon",
							"Moon",
							"The moon is a silver pin-head vast, That holds the heaven's tent-hangings fast.",
							"The moving Moon went up the sky, And nowhere did abide; Softly she was going up, And a star or two beside." });
			put("stars",
					new String[] { "stars", "Stars",
							"Surely the stars are images of love.",
							"The stars are golden fruit upon a tree, All out of reach." });
		}
	};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ConceptFunctionalTest.class,
				concepts.keySet());
	}

	public ConceptFunctionalTest() {
		super();
	}

	public ConceptFunctionalTest(String variation,
			BrowserFunctionalTestCase parent) {
		super(variation, parent);
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
		String[] concept = concepts.get(getVariation());
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

}
