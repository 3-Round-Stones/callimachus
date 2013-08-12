package org.callimachusproject.webdriver;

import junit.framework.TestSuite;

import org.callimachusproject.engine.model.TermFactory;
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
		sendGet(url, 200, "/main-article.docbook?view", purl[3]);
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
		sendGet(url, 301, purl[2], purl[3]);
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
		sendGet(url, 302, purl[2], purl[3]);
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
		sendGet(url, 303, purl[2], purl[3]);
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
		sendGet(url, 307, purl[2], purl[3]);
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
		sendGet(url, 308, purl[2], purl[3]);
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
		sendGet(url, 404, "/main-article.docbook?view", purl[3]);
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
		sendGet(url, 410, "/main-article.docbook?view", purl[3]);
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void sendGet(String url, int code, String location, String cc) throws Exception {
 
		logger.info("Sending 'GET' request to {}", url);
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setInstanceFollowRedirects(false);
		con.setRequestMethod("GET");
		
		// Retrieve Status Code response header and assert
		assertEquals(url, code, con.getResponseCode());
		
		// Retrieve Location response header and print
		String hd = con.getHeaderField("Location");
		if (hd == null) {
			hd = con.getHeaderField("Content-Location");
		}
		assertEquals(url, TermFactory.newInstance(url).resolve(location), hd);
		
		// Retrieve Cache-Control response header and print
		assertEquals(url, cc, con.getHeaderField("Cache-Control"));
	}

}
