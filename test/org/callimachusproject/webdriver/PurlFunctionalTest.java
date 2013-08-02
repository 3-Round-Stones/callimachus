package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.PurlEdit;

public class PurlFunctionalTest extends BrowserFunctionalTestCase {
	public static Map<String, String[]> purls = new LinkedHashMap<String, String[]>() {
		private static final long serialVersionUID = -5837562534292090399L;
		{
			put("index", new String[] { "index.html", "Redirects to home page",
					"/" });
			put("default", new String[] { "default.htm", "", "/" });
		}
	};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(PurlFunctionalTest.class,
				purls.keySet());
	}

	public PurlFunctionalTest() {
		super();
	}

	public PurlFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	public void testCreatePurlAlt(String variation) {
		String[] purl = purls.get(variation);
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
