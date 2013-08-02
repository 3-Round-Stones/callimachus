package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.TextEditor;

public class TextFileFunctionalTest extends BrowserFunctionalTestCase {
	public static final String[] text = new String[] { "text.txt",
			"plain text file" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(TextFileFunctionalTest.class);
	}

	public TextFileFunctionalTest() {
		super();
	}

	public TextFileFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreateTextFile() {
		String name = text[0];
		logger.info("Create text {}", name);
		page.openCurrentFolder().openTextCreate("TextFile").clear()
				.type(text[1]).end().saveAs(name);
		logger.info("Delete text {}", name);
		page.open(name + "?view").openEdit(TextEditor.class).delete();
	}

}
