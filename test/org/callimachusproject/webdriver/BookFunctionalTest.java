/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
		String bookElement = "<book\n"
				+ " xmlns=\"http://docbook.org/ns/docbook\"\n"
				+ " xmlns:xl=\"http://www.w3.org/1999/xlink\"\n"
				+ " xmlns:xi=\"http://www.w3.org/2001/XInclude\"\n"
				+ " version=\"5.0\">\n";
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ bookElement + "<title>" + bookTitle + "</title>\n" + book[2]
				+ "\n</book>";
		page.openCurrentFolder().openTextCreate("Book").setText(markup)
				.saveAs(bookName).waitUntilTitle(bookTitle);
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
		String heading = "<h1>" + articleTitle + "</h1>";
		String body = "<p>" + articleText.replace("\n", "</p>\n<p>") + "</p>";
		page.openCurrentFolder().openArticleCreate().clear()
				.appendHTML(heading).appendHTML(body).saveAs(articleName)
				.waitUntilTitle(articleTitle);
		String bookName = includes[0];
		String bookTitle = includes[1];
		logger.info("Create book {}", bookName);
		String bookElement = "<book\n"
				+ " xmlns=\"http://docbook.org/ns/docbook\"\n"
				+ " xmlns:xl=\"http://www.w3.org/1999/xlink\"\n"
				+ " xmlns:xi=\"http://www.w3.org/2001/XInclude\"\n"
				+ " version=\"5.0\">\n";
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ bookElement + "<title>" + bookTitle + "</title>\n"
				+ includes[2] + "\n</book>";
		page.openCurrentFolder().openTextCreate("Book").setText(markup)
				.saveAs(bookName).waitUntilTitle(bookTitle);
		logger.info("Delete book {}", bookName);
		page.open(bookName + "?view").waitUntilTitle(includes[1])
				.openEdit(TextEditor.class).delete();
		logger.info("Delete article {}", articleName);
		page.open(articleName + "?view").waitUntilTitle(articleTitle)
				.openEdit(DocEditor.class).delete();
	}

}
