package org.callimachusproject.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestSuite;

import org.callimachusproject.webdriver.helpers.BrowserFunctionalTestCase;
import org.callimachusproject.webdriver.helpers.CallimachusDriver;

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
		return BrowserFunctionalTestCase.suite(FolderFunctionalTest.class, folders.keySet());
	}

	public FolderFunctionalTest() {
		super();
	}

	public FolderFunctionalTest(String variation, CallimachusDriver driver) {
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

	public void testCreateFolder() {
		String folderName = folders.get(getVariation());
		driver.createFolder(folderName);
		driver.deleteFolder(folderName);
	}

	public void testFolderEscaping() {
		String folderName = folders.get(getVariation());
		driver.createFolder(folderName);
		for (String concept : ConceptFunctionalTest.concepts.keySet()) {
			new ConceptFunctionalTest(concept, driver).testCreateConcept();
		}
		new BookFunctionalTest(driver).testIncludeArticles();
		for (String purl : PurlFunctionalTest.purls.keySet()) {
			new PurlFunctionalTest(purl, driver).testCreatePurlAlt();
		}
		driver.deleteFolder(folderName);
	}

}
