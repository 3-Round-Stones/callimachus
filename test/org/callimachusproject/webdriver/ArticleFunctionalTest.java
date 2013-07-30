package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.DocEditor;

public class ArticleFunctionalTest extends BrowserFunctionalTestCase {
	public static Map<String, String[]> articles = new LinkedHashMap<String, String[]>() {
		private static final long serialVersionUID = -5837562534292090399L;
		{
			put("ionica",
					new String[] {
							"ionica.docbook",
							"Ionica",
							"They told me, Heraclitus, they told me you were dead,\n"
									+ "They brought me bitter news to hear and bitter tears to shed.\n"
									+ "I wept, as I remembered, how often you and I\n"
									+ "Had tired the sun with talking and sent him down the sky." });
			put("anthologia-polyglotta",
					new String[] {
							"anthologia-polyglotta.docbook",
							"Anthologia Polyglotta",
							"Two goddesses now must Cyprus adore;\n"
									+ "The Muses are ten, the Graces are four;\n"
									+ "Stella's wit is so charming, so sweet her fair face;\n"
									+ "She shines a new Venus, a Muse, and a Grace." });
		}
	};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(ArticleFunctionalTest.class,
				articles.keySet());
	}

	public ArticleFunctionalTest() {
		super();
	}

	public ArticleFunctionalTest(BrowserFunctionalTestCase parent) {
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

	public void testCreateArticle(String variation) {
		String[] article = articles.get(variation);
		String articleName = article[0];
		String articleTitle = article[1];
		logger.info("Create article {}", articleName);
		page.openCurrentFolder().openArticleCreate().clear().type(articleTitle)
				.heading1().type("\n").type(article[2]).saveAs(articleName)
				.waitUntilTitle(articleTitle);
		logger.info("Delete article {}", articleName);
		page.open(articleName + "?view").waitUntilTitle(article[1])
				.openEdit(DocEditor.class).delete();
	}

}
