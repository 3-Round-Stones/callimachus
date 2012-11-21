package org.callimachusproject.types;

import org.callimachusproject.test.TemporaryServerTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.Test;

public class BookTest extends TemporaryServerTestCase {
	private static final String DOCBOOK = "<book version='5.0'  xmlns='http://docbook.org/ns/docbook' xmlns:xl='http://www.w3.org/1999/xlink'> \n" +
			"<title>LS command</title> \n " +
			"<article><title>article title</title><para>This command is a synonym for command. \n" +
			"</para></article> \n </book>";

	@Test
	public void testDocbookCreate() throws Exception {
		WebResource create = getHomeFolder().ref("?create=/callimachus/types/Book&location=test-book.docbook");
		WebResource book = create.create("application/docbook+xml", DOCBOOK.getBytes());
		WebResource edit = book.link("edit-media", "application/docbook+xml");
		edit.get("application/docbook+xml");
		edit.put("application/docbook+xml", DOCBOOK.getBytes());
		book.link("alternate", "application/docbook+xml").get("application/docbook+xml");
	}

}
