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
package org.callimachusproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

public class VersionTest {

	@Test
	public void testRelease() {
		Version version = new Version("0.15");
		assertEquals(0, version.getMajorVersionNum());
		assertEquals(15, version.getReleaseVersionNum());
		assertEquals(0, version.getMaintenanceVersionNum());
		assertEquals(null, version.getQualifierIdentifier());
		assertEquals(0, version.getDevelopmentVersionNum());
	}

	@Test
	public void testmaintenance() {
		Version version = new Version("0.15.1");
		assertEquals(0, version.getMajorVersionNum());
		assertEquals(15, version.getReleaseVersionNum());
		assertEquals(1, version.getMaintenanceVersionNum());
		assertEquals(null, version.getQualifierIdentifier());
		assertEquals(0, version.getDevelopmentVersionNum());
	}

	@Test
	public void testQualifier() {
		Version version = new Version("0.15-alpha-1");
		assertEquals(0, version.getMajorVersionNum());
		assertEquals(15, version.getReleaseVersionNum());
		assertEquals(0, version.getMaintenanceVersionNum());
		assertEquals("alpha", version.getQualifierIdentifier());
		assertEquals(1, version.getDevelopmentVersionNum());
	}

	@Test
	public void testQualifierCompact() {
		Version version = new Version("0.15-alpha1");
		assertEquals(0, version.getMajorVersionNum());
		assertEquals(15, version.getReleaseVersionNum());
		assertEquals(0, version.getMaintenanceVersionNum());
		assertEquals("alpha", version.getQualifierIdentifier());
		assertEquals(1, version.getDevelopmentVersionNum());
	}

	@Test
	public void testDevelopment() {
		Version version = new Version("0.15-1");
		assertEquals(0, version.getMajorVersionNum());
		assertEquals(15, version.getReleaseVersionNum());
		assertEquals(0, version.getMaintenanceVersionNum());
		assertEquals(null, version.getQualifierIdentifier());
		assertEquals(1, version.getDevelopmentVersionNum());
	}

	@Test
	public void testBuild() {
		Version version = new Version("0.15+buildinfo");
		assertEquals(0, version.getMajorVersionNum());
		assertEquals(15, version.getReleaseVersionNum());
		assertEquals(0, version.getMaintenanceVersionNum());
		assertEquals(null, version.getQualifierIdentifier());
		assertEquals(0, version.getDevelopmentVersionNum());
		assertEquals("buildinfo", version.getBuildIdentifier());
	}

	@Test
	public void test() throws Exception {
		Version version = Version.getInstance();
		assertTrue(version.getMajorVersionNum() >= 0);
		assertTrue(version.getReleaseVersionNum() >= 0);
		assertTrue(version.getMaintenanceVersionNum() >= 0);
		version.getQualifierIdentifier();
		assertTrue(version.getDevelopmentVersionNum() >= 0);
		version.getBuildIdentifier();
	}

	@Test
	public void testRoundTrip() throws Exception {
		String version;
		ClassLoader cl = Version.class.getClassLoader();
		InputStream in = cl.getResourceAsStream("META-INF/callimachusproject.properties");
		try {
			Properties result = new Properties();
			result.load(in);
			version = result.getProperty("Version");
			if (version != null) {
				version = version.trim();
			}
		} finally {
			in.close();
		}
		assertEquals(version, Version.getInstance().getVersionCode());
	}

	@Test
	public void testMain() {
		Version.main(new String[0]);
	}

}
