package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;

public class FolderFunctionalTest extends BrowserFunctionalTestCase {
	public static Map<String, String> folders = new LinkedHashMap<String, String>() {
		private static final long serialVersionUID = -5837562534292090399L;
		{
			put("amp", "R&D");
			put("percent", "Bob's%20Folder");
			put("plus", "my+folder");
		}
	};

	public static TestSuite suite() throws Exception {
		return BrowserFunctionalTestCase.suite(FolderFunctionalTest.class,
				folders.keySet());
	}

	public FolderFunctionalTest() {
		super();
	}

	public FolderFunctionalTest(BrowserFunctionalTestCase parent) {
		super(parent);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		String username = getUsername();
		logger.info("Login {}", username);
		page.openLogin().with(username, getPassword()).login();
	}

	@Override
	public void tearDown() throws Exception {
		logger.info("Logout");
		page.logout();
		super.tearDown();
	}

	public void testHomeFolderView() {
		page.openHomeFolder().assertLayout();
	}

	public void testCreateFolder(String variation) {
		String folderName = folders.get(variation);
		logger.info("Create folder {}", folderName);
		page.openCurrentFolder().openFolderCreate().with(folderName).create()
				.waitUntilFolderOpen(folderName);
		logger.info("Delete folder {}", folderName);
		page.openCurrentFolder().waitUntilFolderOpen(folderName).openEdit()
				.waitUntilTitle(folderName).delete();
	}

	public void testFolderEscaping(String variation) {
		String folderName = folders.get(variation);
		logger.info("Create folder {}", folderName);
		page.openCurrentFolder().openFolderCreate().with(folderName).create()
				.waitUntilFolderOpen(folderName);
		for (String concept : ConceptFunctionalTest.concepts.keySet()) {
			new ConceptFunctionalTest(this).testCreateConcept(concept);
		}
		new BookFunctionalTest(this).testIncludeArticles();
		for (String purl : PurlFunctionalTest.purls.keySet()) {
			new PurlFunctionalTest(this).testCreatePurlAlt(purl);
		}
		logger.info("Delete folder {}", folderName);
		page.openCurrentFolder().waitUntilFolderOpen(folderName).openEdit()
				.waitUntilTitle(folderName).delete();
	}

}
