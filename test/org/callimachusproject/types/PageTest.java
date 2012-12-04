package org.callimachusproject.types;

import org.callimachusproject.test.TemporaryServerTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.Test;

public class PageTest extends TemporaryServerTestCase {
	private static final String XHTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
			"<head> <title> Wikipedia </title> </head> \n" +
			"<body> <p> Wikipedia is a great website. </p> </body> </html>";

	@Test
	public void testLess() throws Exception {
		WebResource create = getHomeFolder().ref("?create="+ getCallimachusUrl("types/Page") + "&location=test-page.xhtml");
		WebResource page = create.create("application/xhtml+xml", XHTML.getBytes());
		page.ref("?element=/1&realm=/").get("text/html");
		page.ref("?template&realm=/").get("application/xhtml+xml");
	}

}
