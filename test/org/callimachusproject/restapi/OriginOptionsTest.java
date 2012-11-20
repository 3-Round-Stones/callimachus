package org.callimachusproject.restapi;

import org.callimachusproject.test.TemporaryServerTestCase;

public class OriginOptionsTest extends TemporaryServerTestCase {

	public OriginOptionsTest(String name) throws Exception {
		super(name);
	}
	
	public void test() throws Exception {
		assertNotNull(getHomeFolder().link("contents", "application/atom+xml"));
	}

}
