package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class PageFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] xhtml = new String[] { "page.xhtml", "Page",
			"<h1>Page</h1>\n" + "<p>Paragraph.</p>" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(PageFunctionalTest.class);
	}

	public PageFunctionalTest() {
		super();
	}

	public PageFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	@Override
	public void runBare() throws Throwable {
		locker.writeLock().lock();
		try {
			super.runBare();
		} finally {
			locker.writeLock().unlock();
		}
	}

	public void testCreatePage() {
		String name = xhtml[0];
		logger.info("Create page {}", name);
		String markup = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ "<html\n"
				+ " xmlns=\"http://www.w3.org/1999/xhtml\"\n"
				+ " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
				+ " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
				+ " xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n"
				+ "<head>\n" + "<title>" + xhtml[1] + "</title>\n"
				+ "</head>\n" + "<body>\n" + xhtml[2] + "\n</body>\n</html>";
		page.openCurrentFolder().openTextCreate("Page").clear().type(markup)
				.end().saveAs(name);
		logger.info("Delete page {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
