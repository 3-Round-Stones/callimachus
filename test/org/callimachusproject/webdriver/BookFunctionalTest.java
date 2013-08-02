package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.DocEditor;
import org.callimachusproject.webdriver.pages.TextEditor;

public class BookFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] book = new String[] {
			"book.docbook",
			"Big Book",
			"<article>\n" + "<title>Callimachus</title>\n"
					+ "<para>Big Bore</para>\n" + "</article>" };
	public static final String[] includes = new String[] {
			"callimachus-quotes.docbook",
			"Callimachus Quotes",
			"<xi:include href=\"bionica.docbook\" />\n"
					+ "<xi:include href=\"banthologia-polyglotta.docbook\" />" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(BookFunctionalTest.class);
	}

	public BookFunctionalTest() {
		super();
	}

	public BookFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateBook() {
		String bookName = book[0];
		String bookTitle = book[1];
		logger.info("Create book {}", bookName);
		String bookElement = "<book xmlns=\"http://docbook.org/ns/docbook\" xmlns:xl=\"http://www.w3.org/1999/xlink\" xmlns:xi=\"http://www.w3.org/2001/XInclude\" version=\"5.0\">\n";
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ bookElement + "<title>" + bookTitle + "</title>\n" + book[2]
				+ "\n</book>";
		page.openCurrentFolder().openTextCreate("Book").clear().type(markup)
				.end().saveAs(bookName).waitUntilTitle(bookTitle);
		String bookName1 = book[0];
		logger.info("Delete book {}", bookName1);
		page.open(bookName1 + "?view").waitUntilTitle(book[1])
				.openEdit(TextEditor.class).delete();
	}

	public void testIncludeArticles() {
		for (String[] article : ArticleFunctionalTest.articles.values()) {
			String articleName = "b" + article[0];
			String articleTitle = article[1];
			logger.info("Create article {}", articleName);
			page.openCurrentFolder().openArticleCreate().clear()
					.type(articleTitle).heading1().type("\n").type(article[2])
					.saveAs(articleName).waitUntilTitle(articleTitle);
		}
		String bookName = includes[0];
		String bookTitle = includes[1];
		logger.info("Create book {}", bookName);
		String bookElement = "<book xmlns=\"http://docbook.org/ns/docbook\" xmlns:xl=\"http://www.w3.org/1999/xlink\" xmlns:xi=\"http://www.w3.org/2001/XInclude\" version=\"5.0\">\n";
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ bookElement + "<title>" + bookTitle + "</title>\n"
				+ includes[2] + "\n</book>";
		page.openCurrentFolder().openTextCreate("Book").clear().type(markup)
				.end().saveAs(bookName).waitUntilTitle(bookTitle);
		logger.info("Delete book {}", bookName);
		page.open(bookName + "?view").waitUntilTitle(includes[1])
				.openEdit(TextEditor.class).delete();
		for (String[] article : ArticleFunctionalTest.articles.values()) {
			String articleName = "b" + article[0];
			logger.info("Delete article {}", articleName);
			page.open(articleName + "?view").waitUntilTitle(article[1])
					.openEdit(DocEditor.class).delete();
		}
	}

}
