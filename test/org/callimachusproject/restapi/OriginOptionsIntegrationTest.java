package org.callimachusproject.restapi;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;

public class OriginOptionsIntegrationTest extends TemporaryServerIntegrationTestCase {

	public OriginOptionsIntegrationTest(String name) throws Exception {
		super(name);
	}
	
	public void test() throws Exception {
		assertNotNull(getHomeFolder().link("contents", "application/atom+xml"));
	}

}
