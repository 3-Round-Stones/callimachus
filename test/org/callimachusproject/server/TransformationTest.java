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
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;

public class TransformationTest extends MetadataServerTestCase {

	public static abstract class Service {
		@query("hello")
		@type("text/plain")
		@header("Cache-Control:no-transform")
		@requires("urn:test:grant")
		public String world() {
			return "hello world!";
		}

		@query("hlo")
		@type("text/plain")
		@requires("urn:test:grant")
		public String hlo() {
			return "hello world!";
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(Service.class, RDFS.RESOURCE);
		super.setUp();
	}

	public void testTransformation() {
		WebResource client = Client.create().resource(getOrigin());
		WebResource service = client.path("service").queryParam("hlo", "");
		service.addFilter(new GZIPContentEncodingFilter(true));
		assertEquals("hello world!", service.get(String.class));
	}

	public void testNoTransformationResponse() {
		WebResource client = Client.create().resource(getOrigin());
		WebResource service = client.path("service").queryParam("hello", "");
		assertEquals("hello world!", service.header("Accept-Encoding", "gzip")
				.get(String.class));
	}

	public void testNoTransformationRequest() {
		WebResource client = Client.create().resource(getOrigin());
		WebResource service = client.path("service").queryParam("hlo", "");
		assertEquals("hello world!", service.header("Accept-Encoding", "identity,*;q=0")
				.get(String.class));
	}
}
