package org.callimachusproject.restapi;

import org.callimachusproject.test.TemporaryServerTestCase;

public class FolderContentsTest extends TemporaryServerTestCase {

	public FolderContentsTest(String name) throws Exception {
		super(name);
	}
	
	public void test() throws Exception {
		assertNotNull(getHomeFolder().link("contents", "application/atom+xml").getAppCollection());
	}

}
