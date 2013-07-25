package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

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

	public PurlFunctionalTest(String variation, CallimachusDriver driver) {
		super(variation, driver);
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

	public void testCreatePurlAlt() {
		String[] purl = purls.get(getVariation());
		driver.createPurlAlt(purl[0], purl[1], purl[2]);
		driver.deletePurl(purl[0]);
	}

}
