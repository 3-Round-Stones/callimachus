package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.PurlEdit;

import java.net.HttpURLConnection;
import java.net.URL;

public class PurlFunctionalTest extends BrowserFunctionalTestCase {
	public static String[] purl = { "index.html", "Redirects to home page", "/", "max-age=3600" };

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(PurlFunctionalTest.class);
	}

	public PurlFunctionalTest() {
		super();
	}

	public PurlFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}
	
	public void testCreatePurlCopy() throws Exception {
		String purlName = purl[0];
		String purlType = "Copy (200)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purlType, purl[2], purl[3]).create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlCanonical() throws Exception {
		String purlName = purl[0];
		String purlType = "Canonical (301)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purlType, purl[2], purl[3]).create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}

	public void testCreatePurlAlt() throws Exception {
		String purlName = purl[0];
		String purlType = "Alternate (302)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purlType, purl[2], purl[3]).create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlDescribedBy() throws Exception {
		String purlName = purl[0];
		String purlType = "Described by (303)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purlType, purl[2], purl[3]).create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlResides() throws Exception {
		String purlName = purl[0];
		String purlType = "Resides (307)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purlType, purl[2], purl[3]).create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlMoved() throws Exception {
		String purlName = purl[0];
		String purlType = "Moved (308)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purlType, purl[2], purl[3]).create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlMissing() throws Exception {
		String purlName = purl[0];
		String purlType = "Missing (404)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purlType, purl[2], purl[3]).create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlGone() throws Exception {
		String purlName = purl[0];
		String purlType = "Gone (410)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, purl[1], purlType, purl[2], purl[3]).create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void sendGet(String url) throws Exception {
 
		logger.info("Sending 'GET' request to {}", url);
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		
		// Retrieve Status Code response header and assert
		int responseCode = con.getResponseCode();
		assertEquals(responseCode, 200);
		
		// Retrieve Location response header and print
		String location = con.getHeaderField("Content-Location");
		assertEquals(location, "http://megakakon.3roundstones.net/main-article.docbook?view");
		
		// Retrieve Cache-Control response header and print
		String cacheControl = con.getHeaderField("Cache-Control");
		assertEquals(cacheControl, "max-age=3600");
	}

}
