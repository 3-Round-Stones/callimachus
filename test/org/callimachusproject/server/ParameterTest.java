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
package org.callimachusproject.server;

import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.WebResource;

public class ParameterTest extends MetadataServerTestCase {

	public static abstract class Behaviour {
		@method("GET")
		@query("query")
		@requires("urn:test:grant")
		public String query(@query("q1") String q1) {
			return "hello " + q1;
		}

		@method("GET")
		@query("star")
		@requires("urn:test:grant")
		public String star(@query("*") String qs) {
			return "hello " + qs;
		}

		@method("GET")
		@query("header")
		@requires("urn:test:grant")
		public String header(@header("h1") String h1) {
			return "hello " + h1;
		}

		@method("GET")
		@query("either")
		@requires("urn:test:grant")
		public String either(@query("q1") @header("h1") String value) {
			return "hello " + value;
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(Behaviour.class, RDFS.RESOURCE);
		super.setUp();
	}

	@Override
	protected void addContentEncoding(WebResource client) {
		// don't add gzip support
	}

	public void testJustQuery() throws Exception {
		WebResource q1 = client.path("/").queryParam("query", "").queryParam("q1", "q1");
		assertEquals("hello q1", q1.get(String.class));
	}

	public void testStar() throws Exception {
		WebResource q1 = client.path("/").queryParam("star", "");
		assertEquals("hello star", q1.get(String.class));
	}

	public void testJustHeader() throws Exception {
		WebResource h1 = client.path("/").queryParam("header", "");
		assertEquals("hello h1", h1.header("h1", "h1").get(String.class));
	}

	public void testQuery() throws Exception {
		WebResource q1 = client.path("/").queryParam("either", "").queryParam("q1", "q1");
		assertEquals("hello q1", q1.get(String.class));
	}

	public void testHeader() throws Exception {
		WebResource h1 = client.path("/").queryParam("either", "");
		assertEquals("hello h1", h1.header("h1", "h1").get(String.class));
	}

}
