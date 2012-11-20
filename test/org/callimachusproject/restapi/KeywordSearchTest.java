package org.callimachusproject.restapi;

import org.callimachusproject.test.TemporaryServerTestCase;

public class KeywordSearchTest extends TemporaryServerTestCase {

	public KeywordSearchTest(String name) throws Exception {
		super(name);
	}

	public void testPositiveKeywordSearch() throws Exception {
		String testContents = new String(getHomeFolder().search("test").get("application/atom+xml"));
		assertTrue(testContents.contains("<entry>"));
		
	}
	
	public void testNegativeKeywordSearch() throws Exception {
		String nothingContents = new String(getHomeFolder().search("nothing").get("application/atom+xml"));
		assertFalse(nothingContents.contains("<entry>"));
	}
	
}
