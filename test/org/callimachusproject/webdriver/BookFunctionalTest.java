package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

public class BookFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] book = new String[] {
			"book.docbook",
			"Big Book",
			"<article>\n" + "<title>Callimachus</title>\n"
					+ "<para>Big Bore</para>\n" + "</article>" };
	public static final String[] includes = new String[]{
		"callimachus-quotes.docbook",
		"Callimachus Quotes",
		"<xi:include href=\"ionica.docbook\" />\n"
				+ "<xi:include href=\"anthologia-polyglotta.docbook\" />"};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(BookFunctionalTest.class);
	}

	public BookFunctionalTest() {
		super();
	}

	public BookFunctionalTest(CallimachusDriver driver) {
		super("", driver);
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
		driver.createBook(book[0], book[1], book[2]);
		driver.deleteBook(book[0], book[1]);
	}

	public void testInclude() {
		for (String[] article : ArticleFunctionalTest.articles.values()) {
			driver.createArticle(article[0], article[1], article[2]);
		}
		driver.createBook(includes[0], includes[1], includes[2]);
		driver.deleteBook(includes[0], includes[1]);
		for (String[] article : ArticleFunctionalTest.articles.values()) {
			driver.deleteArticle(article[0], article[1]);
		}
	}

}
