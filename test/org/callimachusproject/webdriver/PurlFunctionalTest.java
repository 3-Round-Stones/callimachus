package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.PurlEdit;

public class PurlFunctionalTest extends BrowserFunctionalTestCase {
	public static String[] purl = { "index.html", "Redirects to home page", "/" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(PurlFunctionalTest.class);
	}

	public PurlFunctionalTest() {
		super();
	}

	public PurlFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreatePurlAlt() {
		String purlName = purl[0];
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purl[2]).create()
				.waitUntilTitle(purlName);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}

}
