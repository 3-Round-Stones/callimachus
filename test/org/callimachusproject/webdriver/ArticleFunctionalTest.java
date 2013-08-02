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
