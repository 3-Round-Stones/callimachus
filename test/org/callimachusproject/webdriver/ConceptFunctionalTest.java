package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

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
							"Surely the stars are images of love." ,"The stars are golden fruit upon a tree, All out of reach." });
		}
	};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ConceptFunctionalTest.class,
				concepts.keySet());
	}

	public ConceptFunctionalTest() {
		super();
	}

	public ConceptFunctionalTest(String variation, CallimachusDriver driver) {
		super(variation, driver);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		driver.login(getUsername(), getPassword());
	}

	@Override
	public void tearDown() throws Exception {
		driver.logout();
		super.tearDown();
	}

	public void testCreateConcept() {
		String[] concept = concepts.get(getVariation());
		driver.createConcept(concept[0], concept[1], concept[2], concept[3]);
		driver.deleteConcept(concept[0], concept[1]);
	}

}
