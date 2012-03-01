package org.callimachusproject;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {

	@Test
	public void testRelease() {
		Version version = new Version("0.15");
		assertTrue(version.getMajorVersionNum() == 0);
		assertTrue(version.getReleaseVersionNum() == 15);
		assertTrue(version.getMaintenanceVersionNum() == 0);
		assertTrue(version.getQualifierVersion() == null);
		assertTrue(version.getDevelopmentVersionNum() == 0);
		Version.main(new String[0]);
	}

	@Test
	public void test() {
		Version version = Version.getInstance();
		assertTrue(version.getMajorVersionNum() >= 0);
		assertTrue(version.getReleaseVersionNum() > 0);
		assertTrue(version.getMaintenanceVersionNum() >= 0);
		version.getQualifierVersion();
		assertTrue(version.getDevelopmentVersionNum() >= 0);
		Version.main(new String[0]);
	}

}
