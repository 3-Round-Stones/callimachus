package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

public class ArticleFunctionalTest extends BrowserFunctionalTestCase {
	public static Map<String, String[]> articles = new LinkedHashMap<String, String[]>() {
		private static final long serialVersionUID = -5837562534292090399L;
		{
			put("ionica",
					new String[] {
							"ionica.docbook",
							"Ionica",
							"They told me, Heraclitus, they told me you were dead,\n" +
							"They brought me bitter news to hear and bitter tears to shed.\n" +
							"I wept, as I remembered, how often you and I\n" +
							"Had tired the sun with talking and sent him down the sky." });
			put("anthologia-polyglotta",
					new String[] {
							"anthologia-polyglotta.docbook",
							"Anthologia Polyglotta",
							"Two goddesses now must Cyprus adore;\n" +
							"The Muses are ten, the Graces are four;\n" +
							"Stella's wit is so charming, so sweet her fair face;\n" +
							"She shines a new Venus, a Muse, and a Grace." });
		}
	};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ArticleFunctionalTest.class,
				articles.keySet());
	}

	public ArticleFunctionalTest() {
		super();
	}

	public ArticleFunctionalTest(String name, CallimachusDriver driver) {
		super(name, driver);
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

	public void testCreate() {
		String[] article = articles.get(getVariation());
		driver.createArticle(article[0], article[1], article[2]);
		driver.deleteArticle(article[0], article[1]);
	}

}
