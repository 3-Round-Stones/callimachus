package org.callimachusproject;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VersionTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		assertTrue(Version.getMajorVersionNum() >= 0);
		assertTrue(Version.getReleaseVersionNum() > 0);
		assertTrue(Version.getMaintenanceVersionNum() >= 0);
		Version.getQualifierVersion();
		assertTrue(Version.getDevelopmentVersionNum() >= 0);
		Version.main(new String[0]);
	}

}
