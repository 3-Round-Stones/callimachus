package org.callimachusproject.fluid;

import junit.framework.TestCase;

import org.apache.http.HttpEntity;

public class FluidTypeTest extends TestCase {

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testQuality() throws Exception {
		FluidType acceptable = new FluidType(HttpEntity.class, "text/html", "application/xhtml+xml", "message/x-response");
		FluidType possible = new FluidType(HttpEntity.class, "text/html", "application/xml", "text/csv", "text/tab-separated-values", "text/xml");
		assertEquals("text/html", possible.as(acceptable).preferred());
	}

}
