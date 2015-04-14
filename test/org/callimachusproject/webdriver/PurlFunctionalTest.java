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

import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.pages.PurlEdit;

import java.net.HttpURLConnection;
import java.net.URL;

public class PurlFunctionalTest extends BrowserFunctionalTestCase {

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(PurlFunctionalTest.class);
	}

	public PurlFunctionalTest() {
		super();
	}

	public PurlFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}
	
	public void testCreatePurl() throws Exception {
		testCreatePurlCopy("copy");
		testCreatePurlCanonical("canonical");
		testCreatePurlAlt("alt");
		testCreatePurlDescribedBy("describedBy");
		testCreatePurlResides("resides");
		testCreatePurlMoved("moved");
		testCreatePurlMissing("missing");
		testCreatePurlGone("gone");
	}
	
	public void testCreatePurlCopy(String purlName) throws Exception {
		String purlType = "Copy (200)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, "Redirects to home page", purlType, "/", "max-age=3600").create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url, 200, "/main-article.docbook?view", "max-age=3600");
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlCanonical(String purlName) throws Exception {
		String purlType = "Canonical (301)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, "Redirects to home page", purlType, "/", "max-age=3600").create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url, 301, "/", "max-age=3600");
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}

	public void testCreatePurlAlt(String purlName) throws Exception {
		String purlType = "Alternate (302)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, "Redirects to home page", purlType, "/", "max-age=3600").create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url, 302, "/", "max-age=3600");
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlDescribedBy(String purlName) throws Exception {
		String purlType = "Described by (303)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, "Redirects to home page", purlType, "/", "max-age=3600").create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url, 303, "/", "max-age=3600");
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlResides(String purlName) throws Exception {
		String purlType = "Resides (307)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, "Redirects to home page", purlType, "/", "max-age=3600").create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url, 307, "/", "max-age=3600");
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlMoved(String purlName) throws Exception {
		String purlType = "Moved (308)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, "Redirects to home page", purlType, "/", "max-age=3600").create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url, 308, "/", "max-age=3600");
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlMissing(String purlName) throws Exception {
		String purlType = "Missing (404)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, "Redirects to home page", purlType, "/", "max-age=3600").create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url, 404, "/main-article.docbook?view", "max-age=3600");
		logger.info("Delete purl {}", purlName);
		page.open(purlName + "?view").waitUntilTitle(purlName)
				.openEdit(PurlEdit.class).delete(purlName);
	}
	
	public void testCreatePurlGone(String purlName) throws Exception {
		String purlType = "Gone (410)";
		logger.info("Create purl {}", purlName);
		page.openCurrentFolder().openPurlCreate()
				.with(purlName, "Redirects to home page", purlType, "/", "max-age=3600").create()
				.waitUntilTitle(purlName);
		String url = page.getCurrentUrl();
		url = url.replace("?view", "");
		sendGet(url, 410, "/main-article.docbook?view", "max-age=3600");
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
		con.setRequestProperty("Accept-Encoding", "gzip");
		
		// Retrieve Status Code response header and assert
		assertEquals(url, code, con.getResponseCode());
		
		// Retrieve Location response header and print
		String hd = con.getHeaderField("Location");
		if (hd == null) {
			hd = con.getHeaderField("Content-Location");
		}
		assertEquals(url, TermFactory.newInstance(url).resolve(location), TermFactory.newInstance(url).resolve(hd));
		
		// Retrieve Cache-Control response header and print
		String actual = con.getHeaderField("Cache-Control");
		if (actual == null || !actual.contains(cc)) {
			assertEquals(url, cc, actual);
		}
	}

}
