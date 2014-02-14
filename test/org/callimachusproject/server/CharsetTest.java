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

import java.io.IOException;
import java.nio.CharBuffer;

import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class CharsetTest extends MetadataServerTestCase {

	public static class Resource {
		@query("string")
		@requires("urn:test:grant")
		@type("text/plain")
		public String hello() {
			return "Hello World!";
		}

		@query("stream")
		@requires("urn:test:grant")
		@type("text/plain; charset=UTF-8")
		public Readable stream() {
			return new Readable() {
				private boolean written;
				public int read(CharBuffer cb) throws IOException {
					if (written)
						return -1;
					written = true;
					cb.append("Hello World!");
					return "Hello World!".length();
				}
			};
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Resource.class, RDFS.RESOURCE);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
	}

	public void testCharsetStream() throws Exception {
		client.path("/hello").put("resource");
		WebResource web = client.path("/hello").queryParam("stream", "");
		ClientResponse get = web.header("Accept-Charset", "ISO-8859-1").get(
				ClientResponse.class);
		assertEquals("text/plain; charset=UTF-8", get.getHeaders().getFirst(
				"Content-Type"));
	}
}
