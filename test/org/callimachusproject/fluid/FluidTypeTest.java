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
		FluidType acceptable = new FluidType(HttpEntity.class, "text/html", "application/xhtml+xml", "image/webp", "application/xml", "*/*");
		FluidType rdf = acceptable.as(new FluidType(HttpEntity.class, "text/turtle;q=0.06", "application/ld+json;q=0.04", "application/rdf+xml;q=0.02"));
		FluidType turtle = acceptable.as(new FluidType(HttpEntity.class, "text/turtle"));
		assertTrue(turtle.getQuality() > rdf.getQuality());
	}

	public void testPreferred() throws Exception {
		FluidType acceptable = new FluidType(HttpEntity.class, "text/html", "application/xhtml+xml", "message/x-response");
		FluidType possible = new FluidType(HttpEntity.class, "text/html", "application/xml", "text/csv", "text/tab-separated-values", "text/xml");
		assertEquals("text/html", possible.as(acceptable).preferred());
	}

}
