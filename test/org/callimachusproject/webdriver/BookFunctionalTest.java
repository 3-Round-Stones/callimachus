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
			"callimachus-quotes.docbook", "Callimachus Quotes",
			"<xi:include href=\"ionica.docbook\" />" };

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
		String articleName = "ionica.docbook";
		String articleTitle = ArticleFunctionalTest.article[1];
		String articleText = ArticleFunctionalTest.article[2];
		logger.info("Create article {}", articleName);
		page.openCurrentFolder().openArticleCreate().clear().type(articleTitle)
				.heading1().type("\n").type(articleText).saveAs(articleName)
				.waitUntilTitle(articleTitle);
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
		logger.info("Delete article {}", articleName);
		page.open(articleName + "?view").waitUntilTitle(articleTitle)
				.openEdit(DocEditor.class).delete();
	}

}
