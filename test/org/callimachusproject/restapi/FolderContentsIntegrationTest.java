package org.callimachusproject.restapi;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;

public class FolderContentsIntegrationTest extends TemporaryServerIntegrationTestCase {

	public FolderContentsIntegrationTest(String name) throws Exception {
		super(name);
	}
	
	public void test() throws Exception {
		assertNotNull(getHomeFolder().link("contents", "application/atom+xml").getAppCollection());
	}

}
